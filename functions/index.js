const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

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

        console.log(`Nuevo miembro ${memberId} añadido al tablero ${boardId}. Preparando notificación.`);

        // 1. OBTENER DATOS DEL USUARIO
        const userDoc = await admin.firestore().collection("users").doc(memberId).get();
        if (!userDoc.exists) {
            console.error(`Error: Documento de usuario no encontrado para el ID: ${memberId}`);
            return null;
        }
        const userData = userDoc.data();

        // 2. VERIFICAR PREFERENCIAS Y TOKEN
        if (userData.notificationsEnabled === false) {
            console.log(`El usuario ${memberId} tiene las notificaciones desactivadas. Abortando.`);
            return null;
        }
        if (!userData.fcmToken) {
            console.log(`El usuario ${memberId} no tiene un token de FCM registrado. Abortando.`);
            return null;
        }

        // 3. CONSTRUIR EL MENSAJE
        let boardName = "un tablero"; // Valor por defecto
        const boardDoc = await admin.firestore().collection("boards").doc(boardId).get();
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
