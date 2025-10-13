package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.utp.wemake.models.Board;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.BoardRepository;
import com.utp.wemake.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BoardViewModel extends ViewModel {
    private ListenerRegistration boardListener;
    private ListenerRegistration memberDetailsListener;
    private final BoardRepository boardRepository = new BoardRepository();
    private final UserRepository userRepository = new UserRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Member> currentUserMemberDetails = new MutableLiveData<>();
    private final MutableLiveData<Board> boardData = new MutableLiveData<>();
    private final MutableLiveData<List<User>> boardMembers = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Board> getBoardData() { return boardData; }
    public LiveData<List<User>> getBoardMembers() { return boardMembers; }
    public LiveData<Member> getCurrentUserMemberDetails() { return currentUserMemberDetails; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }
    public LiveData<String> getError() { return error; }

    /**
     * Carga los datos de un tablero existente. Llamado en modo "Editar".
     */
    public void loadBoard(String boardId) {
        boardRepository.getBoardById(boardId).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Board board = documentSnapshot.toObject(Board.class);
                boardData.setValue(board);

                if (board != null && board.getMembers() != null && !board.getMembers().isEmpty()) {
                    loadBoardMembers(board.getMembers());
                }

            } else {
                error.setValue("El tablero no fue encontrado.");
            }
        }).addOnFailureListener(e -> error.setValue("Error al cargar el tablero."));
    }

    private void loadBoardMembers(List<String> memberIds) {
        userRepository.getUsersByIds(memberIds).addOnSuccessListener(users -> {
            boardMembers.setValue(users);
        }).addOnFailureListener(e -> {
            error.setValue("No se pudieron cargar los integrantes.");
        });
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

    public void listenToBoard(String boardId) {
        removeListeners();

        String currentUserId = auth.getUid();
        if (boardId == null || currentUserId == null) {
            error.setValue("Faltan datos (boardId o userId) para iniciar la escucha.");
            return;
        }

        boardListener = boardRepository.listenToBoardById(boardId, (snapshot, e) -> {
            if (e != null) {
                this.error.setValue("Error al escuchar el tablero.");
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Board board = snapshot.toObject(Board.class);
                boardData.setValue(board);
                if (board != null && board.getMembers() != null && !board.getMembers().isEmpty()) {
                    loadBoardMembers(board.getMembers());
                } else {
                    boardMembers.setValue(new ArrayList<>());
                }
            } else {
                this.error.setValue("El tablero no fue encontrado o fue eliminado.");
            }
        });

        memberDetailsListener = boardRepository.listenToMemberDetails(boardId, currentUserId, (snapshot, e) -> {
            if (e != null) {
                this.error.setValue("Error al obtener el rol del usuario.");
                currentUserMemberDetails.setValue(null);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentUserMemberDetails.setValue(snapshot.toObject(Member.class));
            } else {
                currentUserMemberDetails.setValue(null);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (boardListener != null) {
            boardListener.remove();
        }
    }

    private void removeListeners() {
        if (boardListener != null) {
            boardListener.remove();
            boardListener = null;
        }
        if (memberDetailsListener != null) {
            memberDetailsListener.remove();
            memberDetailsListener = null;
        }
    }

    public void deleteBoard(String boardId) {
        boardRepository.deleteBoard(boardId).addOnCompleteListener(task -> {
            operationSuccess.setValue(task.isSuccessful());
            if (!task.isSuccessful()) error.setValue("Error al eliminar el tablero.");
        });
    }
}