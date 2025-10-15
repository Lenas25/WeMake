package com.utp.wemake.viewmodels;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Member; // Importar el modelo Member
import com.utp.wemake.models.User;
import com.utp.wemake.repository.BoardRepository; // Importar BoardRepository
import com.utp.wemake.repository.ImageRepository;
import com.utp.wemake.repository.UserRepository;

public class ProfileViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final ImageRepository imageRepository = new ImageRepository();
    private final MutableLiveData<String> _profilePictureUrlUpdated = new MutableLiveData<>();
    private final BoardRepository boardRepository = new BoardRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Lo reemplazamos con LiveData para los detalles del miembro.
    private final MutableLiveData<Member> memberDetails = new MutableLiveData<>();
    public LiveData<String> getProfilePictureUrlUpdated() {
        return _profilePictureUrlUpdated;
    }

    // LiveData existentes
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel() {
        // La carga de 'loadUserData' se elimina de aquí.
    }

    // --- Getters para que la Vista los observe ---
    public LiveData<Member> getMemberDetails() { return memberDetails; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /**
     * Carga los detalles específicos del miembro (rol, puntos) para un tablero dado.
     * Este método será llamado por el ProfileFragment cuando el tablero seleccionado cambie.
     * @param boardId El ID del tablero seleccionado.
     */
    public void loadMemberDetailsForBoard(String boardId) {
        String currentUserId = auth.getUid();
        if (boardId == null || currentUserId == null) {
            errorMessage.setValue("No se puede cargar el perfil sin un tablero o usuario.");
            memberDetails.setValue(new Member("member", 0)); // Mostramos 0 puntos por defecto
            return;
        }

        isLoading.setValue(true);
        boardRepository.getMemberDetails(boardId, currentUserId).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                memberDetails.setValue(task.getResult());
            } else {
                errorMessage.setValue("Error al cargar los puntos de este tablero.");
                memberDetails.setValue(new Member("member", 0)); // Mostramos 0 puntos si hay error
            }
        });
    }

    /**
     * Inicia el proceso de actualización de la foto de perfil.
     */
    public void updateUserProfilePicture(Uri imageUri, User currentUser) {
        if (currentUser == null) {
            errorMessage.setValue("Error: no se pueden guardar los cambios sin datos de usuario.");
            return;
        }
        isLoading.setValue(true);

        imageRepository.uploadImage(imageUri, new ImageRepository.UploadCallbackListener() {
            @Override
            public void onSuccess(String imageUrl) {
                updatePhotoUrlInFirestore(imageUrl, currentUser);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue("Error al subir la imagen: " + error);
            }
        });
    }

    /**
     * Guarda la nueva URL de la imagen en el documento del usuario en Firestore.
     */
    private void updatePhotoUrlInFirestore(String newPhotoUrl, User userToUpdate) {
        userToUpdate.setPhotoUrl(newPhotoUrl);

        userRepository.updateProfileData(userToUpdate).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                _profilePictureUrlUpdated.setValue(newPhotoUrl);
            } else {
                errorMessage.setValue("Error al guardar la nueva foto en el perfil.");
            }
        });
    }

    /**
     * Llama al repositorio para actualizar la preferencia de notificaciones del usuario en Firestore.
     * @param isEnabled El nuevo estado de la preferencia.
     */
    public void updateNotificationPreference(boolean isEnabled) {
        // Simplemente pasamos la llamada al repositorio
        userRepository.updateNotificationPreference(isEnabled).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                errorMessage.setValue("Error al guardar la preferencia.");
            }
        });
    }



}