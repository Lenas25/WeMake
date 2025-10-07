package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Board;
import com.utp.wemake.repository.BoardRepository;
import java.util.Collections;
import java.util.UUID;

public class CreateBoardViewModel extends ViewModel {

    private final BoardRepository boardRepository = new BoardRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Boolean> isBoardCreated = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Boolean> getIsBoardCreated() { return isBoardCreated; }
    public LiveData<String> getError() { return error; }

    public void createNewBoard(String boardName) {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            error.setValue("No se pudo crear el tablero: usuario no autenticado.");
            return;
        }


        String inviteCode = generateInviteCode();

        // Preparamos los datos del nuevo tablero
        String defaultColor = "#C1CD7D";

        Board newBoard = new Board(
                boardName,
                null,
                defaultColor,
                Collections.singletonList(currentUserId)
        );

        newBoard.setInvitationCode(inviteCode);

        boardRepository.createBoard(newBoard).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isBoardCreated.setValue(true);
            } else {
                isBoardCreated.setValue(false);
                error.setValue("Ocurrió un error al crear el tablero.");
            }
        });
    }

    /**
     * Genera un código alfanumérico aleatorio de 6 caracteres.
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}