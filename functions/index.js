/* eslint-disable linebreak-style */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();


const db = admin.firestore();

/**
 * Se dispara cuando se crea un nuevo documento de miembro
 * en la subcolección 'members_details' de cualquier tablero.
 */
exports.sendNotificationOnNewMember = functions.firestore
    // ----- LA ÚNICA LÍNEA QUE CAMBIA ES ESTA -----
    .document("boards/{boardId}/members_details/{memberId}")
    // ---------------------------------------------
    .onCreate(async (snapshot, context) => {
      // El resto del código es idéntico y correcto
      const memberId = context.params.memberId;
      const boardId = context.params.boardId;

      console.log(`Nuevo miembro ${memberId} añadido al 
        tablero ${boardId}. Preparando notificación.`);

      // 1. OBTENER DATOS DEL USUARIO
      const userDoc = await admin.firestore().
          collection("users").
          doc(memberId).get();
      if (!userDoc.exists) {
        console.error(`Error: Documento de usuario 
            no encontrado para el ID: ${memberId}`);
        return null;
      }
      const userData = userDoc.data();

      // 2. VERIFICAR PREFERENCIAS Y TOKEN
      if (userData.notificationsEnabled === false) {
        console.log(`El usuario ${memberId} tiene las 
            notificaciones desactivadas. Abortando.`);
        return null;
      }
      if (!userData.fcmToken) {
        console.log(`El usuario ${memberId} no tiene un 
            token de FCM registrado. Abortando.`);
        return null;
      }

      // 3. CONSTRUIR EL MENSAJE
      let boardName = "un tablero"; // Valor por defecto
      const boardDoc = await admin.firestore().
          collection("boards").
          doc(boardId).get();
      if (boardDoc.exists) {
        boardName = boardDoc.data().name || boardName;
      }

      const payload = {
        notification: {
          title: "¡Te han añadido a un tablero!",
          body: `Has sido agregado al tablero '${boardName}'.`,
        },
        token: userData.fcmToken,
      };

      // 4. ENVIAR NOTIFICACIÓN
      try {
        await admin.messaging().send(payload);
        console.log(`Notificación enviada con éxito a ${memberId}`);
      } catch (error) {
        console.error(`Error al enviar la notificación a ${memberId}:`, error);
      }

      return null;
    });

exports.applyPenaltiesForOverdueTasks = functions.pubsub
    .schedule("every 1 hours")
    .onRun(async (context) => {
      console.log("Ejecutando la comprobación de tareas vencidas...");

      const now = new Date();
      const tasksRef = db.collection("tasks");

      // 1. LA CONSULTA: Buscar tareas que cumplen todas estas condiciones:
      //    - La fecha límite (deadline) ya pasó.
      //    - El estado NO es 'completed'.
      //    - La penalidad NO ha sido aplicada
      // todavía (para evitar penalizar dos veces).
      const query = tasksRef
          .where("deadline", "<=", now)
          .where("status", "!=", "completed")
          .where("penaltyApplied", "==", false);

      const overdueTasksSnapshot = await query.get();

      if (overdueTasksSnapshot.empty) {
        console.log("No se encontraron tareas vencidas.");
        return null;
      }

      console.log(`Se encontraron ${overdueTasksSnapshot.size} 
        tareas vencidas para procesar.`);

      // 2. PROCESAR CADA TAREA VENCIDA
      // Usamos un array de promesas para manejar todas
      // las actualizaciones de forma concurrente.
      const promises = [];

      overdueTasksSnapshot.forEach((taskDoc) => {
        const taskData = taskDoc.data();
        const taskId = taskDoc.id;

        // Verificación de seguridad: nos aseguramos de que
        // la tarea tenga los datos necesarios
        if (!taskData.boardId ||
            !taskData.assignedMembers ||
            taskData.penaltyPoints <= 0) {
          console.log(`Saltando tarea ${taskId} por
            falta de datos (boardId, assignedMembers, o penaltyPoints > 0).`);
          return; // 'return' aquí es como 'continue' en un bucle forEach
        }

        const promise = applyPenalty(taskDoc.ref, taskData);
        promises.push(promise);
      });

      // Esperar a que todas las actualizaciones terminen
      await Promise.all(promises);
      console.log("Procesamiento de tareas vencidas completado.");
      return null;
    });

/**
 * Función auxiliar que aplica la penalidad a una
 * tarea específica usando un Write Batch.
 * @param {FirebaseFirestore.DocumentReference}
 * taskRef - La referencia al documento de la tarea.
 * @param {object} taskData - Los datos del documento de la tarea.
 */
async function applyPenalty(taskRef, taskData) {
  const batch = db.batch();

  // A. Marcar la tarea para que no se vuelva a penalizar
  batch.update(taskRef, {penaltyApplied: true});

  // B. Restar los puntos a cada miembro asignado
  const penalty = taskData.penaltyPoints;
  taskData.assignedMembers.forEach((memberId) => {
    const memberRef = db.collection("boards").doc(taskData.boardId)
        .collection("members_details").doc(memberId);

    // Usamos FieldValue.increment con un número
    // negativo para restar de forma segura
    batch.update(memberRef, {points: admin.
        firestore.FieldValue.increment(-penalty)});
  });

  try {
    await batch.commit();
    console.log(`Penalidad de ${penalty} puntos 
        aplicada con éxito a la tarea ${taskRef.id}.`);
  } catch (error) {
    console.error(`Error al aplicar 
        la penalidad a la tarea ${taskRef.id}:`, error);
  }
}
