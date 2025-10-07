package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Board;
import com.utp.wemake.repository.BoardRepository;
import java.util.Collections;
import java.util.UUID;

public class BoardViewModel extends ViewModel {
    private final BoardRepository boardRepository = new BoardRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Board> boardData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Board> getBoardData() { return boardData; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }
    public LiveData<String> getError() { return error; }

    /**
     * Carga los datos de un tablero existente. Llamado en modo "Editar".
     */
    public void loadBoard(String boardId) {
        boardRepository.getBoardById(boardId).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                boardData.setValue(documentSnapshot.toObject(Board.class));
            } else {
                error.setValue("El tablero no fue encontrado.");
            }
        }).addOnFailureListener(e -> error.setValue("Error al cargar el tablero."));
    }

    /**
     * Guarda un tablero. Decide si es una creación o una actualización.
     */
    public void saveBoard(String boardId, String name, String description, String color) {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            error.setValue("Usuario no autenticado.");
            return;
        }

        if (boardId == null) {
            // MODO CREAR
            String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            Board newBoard = new Board(name, description, color, Collections.singletonList(currentUserId));
            newBoard.setInvitationCode(inviteCode);

            boardRepository.createBoard(newBoard).addOnCompleteListener(task -> {
                operationSuccess.setValue(task.isSuccessful());
                if (!task.isSuccessful()) error.setValue("Error al crear el tablero.");
            });
        } else {
            // MODO EDITAR
            Board updatedBoard = boardData.getValue(); // Usa los datos existentes como base
            if (updatedBoard == null) return;

            updatedBoard.setName(name);
            updatedBoard.setDescription(description);
            updatedBoard.setColor(color);

            boardRepository.updateBoard(boardId, updatedBoard).addOnCompleteListener(task -> {
                operationSuccess.setValue(task.isSuccessful());
                if (!task.isSuccessful()) error.setValue("Error al actualizar el tablero.");
            });
        }
    }

    public void deleteBoard(String boardId) {
        boardRepository.deleteBoard(boardId).addOnCompleteListener(task -> {
            operationSuccess.setValue(task.isSuccessful());
            if (!task.isSuccessful()) error.setValue("Error al eliminar el tablero.");
        });
    }
}