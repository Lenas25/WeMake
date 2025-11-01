package com.utp.wemake.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Board;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.BoardRepository;
import com.utp.wemake.utils.BoardSelectionPrefs;
import com.utp.wemake.repository.UserRepository;
import com.utp.wemake.utils.Event;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    public enum JoinBoardResult {
        SUCCESS,
        ALREADY_MEMBER,
        BOARD_NOT_FOUND,
        ERROR
    }
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardSelectionPrefs boardSelectionRepo;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<User> currentUserData = new MutableLiveData<>();
    private final MutableLiveData<List<Board>> userBoards = new MutableLiveData<>();
    private final MutableLiveData<Board> selectedBoard = new MutableLiveData<>();
    private final MutableLiveData<Event<JoinBoardResult>> joinBoardResult = new MutableLiveData<>();

    // Getter para que el Fragment lo observe
    public LiveData<Event<JoinBoardResult>> getJoinBoardResult() {
        return joinBoardResult;
    }


    public MainViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
        boardRepository = new BoardRepository();
        boardSelectionRepo = new BoardSelectionPrefs(application);
        loadInitialData();
    }

    // --- Getters ---
    public LiveData<User> getCurrentUserData() { return currentUserData; }
    public LiveData<List<Board>> getUserBoards() { return userBoards; }
    public LiveData<Board> getSelectedBoard() { return selectedBoard; }

    private void loadInitialData() {
        loadCurrentUser();
        loadUserBoards();
    }

    /**
     * Carga los datos del perfil del usuario actual desde Firestore.
     */
    private void loadCurrentUser() {
        userRepository.getCurrentUserData().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                // CAMBIO: Actualizamos el LiveData correcto
                currentUserData.setValue(user);
            } else {
                // Opcional: Manejar error, por ejemplo, publicando un valor nulo
                currentUserData.setValue(null);
            }
        });
    }

    private void loadUserBoards() {
        boardRepository.getBoardsForCurrentUser().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Board> boards = task.getResult().toObjects(Board.class);
                userBoards.setValue(boards);

                // Después de cargar los tableros, determina cuál está seleccionado
                determineInitialSelectedBoard(boards);
            }
        });
    }

    public void updateUserPhotoLocally(String newPhotoUrl) {
        User currentUser = currentUserData.getValue();

        if (currentUser != null) {
            currentUser.setPhotoUrl(newPhotoUrl);

            currentUserData.setValue(currentUser);
        }
    }


    private void determineInitialSelectedBoard(List<Board> boards) {
        if (boards == null || boards.isEmpty()) return;

        String savedBoardId = boardSelectionRepo.getSelectedBoardId();
        Board boardToSelect = null;

        if (savedBoardId != null) {
            // Intenta encontrar el tablero guardado en la lista actual
            boardToSelect = boards.stream().filter(b -> b.getId().equals(savedBoardId)).findFirst().orElse(null);
        }

        // Si no hay un tablero guardado, o si el tablero guardado ya no existe, selecciona el primero de la lista.
        if (boardToSelect == null) {
            boardToSelect = boards.get(0);
            // Guardar automáticamente el boardId seleccionado por defecto
            boardSelectionRepo.saveSelectedBoardId(boardToSelect.getId());
        }

        // Publica el tablero seleccionado
        selectedBoard.setValue(boardToSelect);
    }

    /**
     * Lo llama la UI cuando el usuario elige un nuevo tablero del dropdown.
     */
    public void selectBoard(Board board) {
        selectedBoard.setValue(board);
        boardSelectionRepo.saveSelectedBoardId(board.getId());
    }

    /**
     * Intenta unir al usuario actual a un tablero usando un código de invitación,
     * verificando primero si ya es miembro.
     * @param code El código de 6 caracteres.
     */
    public void joinBoardWithCode(String code) {
        if (code == null || code.trim().length() != 6) {
            joinBoardResult.setValue(new Event<>(JoinBoardResult.ERROR));
            return;
        }

        String upperCaseCode = code.trim().toUpperCase();
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            joinBoardResult.setValue(new Event<>(JoinBoardResult.ERROR));
            return;
        }

        boardRepository.findBoardByInviteCode(upperCaseCode).addOnCompleteListener(findTask -> {
            if (findTask.isSuccessful() && findTask.getResult() != null && !findTask.getResult().isEmpty()) {
                String boardId = findTask.getResult().getDocuments().get(0).getId();

                List<Board> currentBoards = userBoards.getValue();
                boolean isAlreadyMember = false;
                if (currentBoards != null) {
                    isAlreadyMember = currentBoards.stream().anyMatch(board -> board.getId().equals(boardId));
                }

                if (isAlreadyMember) {
                    joinBoardResult.setValue(new Event<>(JoinBoardResult.ALREADY_MEMBER));
                    return;
                }

                boardRepository.joinBoard(boardId, currentUserId).addOnCompleteListener(joinTask -> {
                    if (joinTask.isSuccessful()) {
                        loadUserBoards();
                        joinBoardResult.setValue(new Event<>(JoinBoardResult.SUCCESS));
                    } else {
                        joinBoardResult.setValue(new Event<>(JoinBoardResult.ERROR));
                    }
                });

            } else if (findTask.isSuccessful()) {
                joinBoardResult.setValue(new Event<>(JoinBoardResult.BOARD_NOT_FOUND));
            } else {
                joinBoardResult.setValue(new Event<>(JoinBoardResult.ERROR));
            }
        });
    }

}