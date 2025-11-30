package com.utp.wemake.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.utp.wemake.models.Board;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.BoardRepository;
import com.utp.wemake.utils.BoardSelectionPrefs;
import com.utp.wemake.repository.UserRepository;
import com.utp.wemake.utils.Event;

import java.util.ArrayList;
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

    // Listener para los tableros en tiempo real
    private ListenerRegistration boardsListener;

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
        listenToUserBoards(); // Usar listener en tiempo real
    }

    /**
     * Carga los datos del perfil del usuario actual desde Firestore.
     */
    private void loadCurrentUser() {
        userRepository.getCurrentUserData().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                currentUserData.setValue(user);
            } else {
                currentUserData.setValue(null);
            }
        });
    }

    /**
     * Escucha en tiempo real los tableros del usuario.
     * Se actualiza automáticamente cuando se crean o eliminan tableros.
     */
    private void listenToUserBoards() {
        String uid = auth.getUid();
        if (uid == null) return;

        // Detener listener anterior si existe
        if (boardsListener != null) {
            boardsListener.remove();
        }

        // Obtener el Query desde el repositorio
        Query boardsQuery = boardRepository.getBoardsQueryForCurrentUser();
        if (boardsQuery == null) return;

        // Crear listener en tiempo real
        boardsListener = boardsQuery.addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Log.e("MainViewModel", "Error listening to boards", e);
                return;
            }

            if (querySnapshot != null) {
                List<Board> boards = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Board board = doc.toObject(Board.class);
                    if (board != null) {
                        board.setId(doc.getId());
                        boards.add(board);
                    }
                }

                userBoards.setValue(boards);

                // Determinar el tablero seleccionado
                determineSelectedBoard(boards);
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

    /**
     * Determina qué tablero debe estar seleccionado.
     * Prioriza el tablero guardado, luego el actual si sigue existiendo, o el primero disponible.
     */
    private void determineSelectedBoard(List<Board> boards) {
        if (boards == null || boards.isEmpty()) {
            selectedBoard.setValue(null);
            return;
        }

        String savedBoardId = boardSelectionRepo.getSelectedBoardId();
        Board currentSelected = selectedBoard.getValue();
        Board boardToSelect = null;

        // 1. Intentar usar el tablero guardado en preferencias
        if (savedBoardId != null) {
            boardToSelect = boards.stream()
                    .filter(b -> b.getId().equals(savedBoardId))
                    .findFirst()
                    .orElse(null);
        }

        // 2. Si no existe, intentar mantener el tablero actualmente seleccionado
        if (boardToSelect == null && currentSelected != null) {
            boardToSelect = boards.stream()
                    .filter(b -> b.getId().equals(currentSelected.getId()))
                    .findFirst()
                    .orElse(null);
        }

        // 3. Si tampoco existe, seleccionar el primero de la lista
        if (boardToSelect == null) {
            boardToSelect = boards.get(0);
            boardSelectionRepo.saveSelectedBoardId(boardToSelect.getId());
        }

        // Solo actualizar si cambió el tablero seleccionado
        if (currentSelected == null || !currentSelected.getId().equals(boardToSelect.getId())) {
            selectedBoard.setValue(boardToSelect);
        }
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
                        // No necesitamos llamar loadUserBoards() porque el listener se actualizará automáticamente
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

    /**
     * Limpia el listener cuando el ViewModel se destruye.
     * CRÍTICO para evitar memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        // Detener el listener cuando el ViewModel se destruye
        if (boardsListener != null) {
            boardsListener.remove();
        }
    }
}