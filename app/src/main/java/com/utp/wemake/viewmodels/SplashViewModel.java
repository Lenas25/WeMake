package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utp.wemake.repository.BoardRepository;
import com.utp.wemake.repository.UserRepository;

public class SplashViewModel extends ViewModel {

    public enum Destination { LOGIN, SETUP, MAIN }

    private final MutableLiveData<Destination> destination = new MutableLiveData<>();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final BoardRepository boardRepository = new BoardRepository();
    private final UserRepository userRepository = new UserRepository();

    public LiveData<Destination> getDestination() { return destination; }


    public void decideNextScreen() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            // No hay nadie en el caché, definitivamente no está logueado.
            destination.setValue(Destination.LOGIN);
            return;
        }

        // Hay un usuario en el caché. Vamos a verificar si su cuenta todavía es válida en el servidor.
        currentUser.reload().addOnCompleteListener(reloadTask -> {
            if (reloadTask.isSuccessful()) {
                // El usuario es válido en el servidor. Ahora procedemos con la lógica normal.
                checkUserDocumentsAndBoards();
            } else {
                // La recarga falló. Esto significa que el usuario fue eliminado
                handleAuthError();
            }
        });
    }

    private void checkUserDocumentsAndBoards() {
        userRepository.doesUserDocumentExist().addOnCompleteListener(userTask -> {
            if (userTask.isSuccessful() && userTask.getResult().exists()) {
                // El documento SÍ existe. Ahora, comprueba si tiene tableros.
                boardRepository.getBoardsForCurrentUser().addOnCompleteListener(boardTask -> {
                    if (boardTask.isSuccessful() && boardTask.getResult() != null) {
                        destination.setValue(boardTask.getResult().isEmpty() ? Destination.SETUP : Destination.MAIN);
                    } else {
                        handleAuthError();
                    }
                });
            } else {
                // El documento NO existe. La cuenta está desincronizada.
                handleAuthError();
            }
        });
    }


    /**
     * Método de ayuda para manejar errores de autenticación/sincronización.
     */
    private void handleAuthError() {
        auth.signOut();
        destination.setValue(Destination.LOGIN);
    }
}