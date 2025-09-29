package com.utp.wemake.viewmodels;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.ImageRepository;
import com.utp.wemake.repository.UserRepository;

public class ProfileViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final ImageRepository imageRepository = new ImageRepository();

    // LiveData para los datos del usuario que la vista observará
    private final MutableLiveData<User> userData = new MutableLiveData<>();
    // LiveData para el estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    // LiveData para mensajes de error
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel() {
        loadUserData();
    }

    // --- Getters para que la Vista los observe ---
    public LiveData<User> getUserData() { return userData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /**
     * Carga los datos del perfil del usuario actual desde Firestore.
     */
    public void loadUserData() {
        isLoading.setValue(true);
        userRepository.getCurrentUserData().addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                userData.setValue(user);
            } else {
                errorMessage.setValue("Error al cargar el perfil.");
            }
        });
    }

    /**
     * Inicia el proceso de actualización de la foto de perfil.
     */
    public void updateUserProfilePicture(Uri imageUri) {
        isLoading.setValue(true);

        // 1. Sube la imagen a Cloudinary
        imageRepository.uploadImage(imageUri, new ImageRepository.UploadCallbackListener() {
            @Override
            public void onSuccess(String imageUrl) {
                // 2. Si la subida es exitosa, actualiza la URL en Firestore
                updatePhotoUrlInFirestore(imageUrl);
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
    private void updatePhotoUrlInFirestore(String newPhotoUrl) {
        User currentUser = userData.getValue();
        if (currentUser == null) {
            isLoading.setValue(false);
            errorMessage.setValue("No se pudieron obtener los datos del usuario actual.");
            return;
        }

        currentUser.setPhotoUrl(newPhotoUrl);

        // Llama al método del repositorio para actualizar el perfil
        userRepository.updateProfileData(currentUser).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                // Notifica a la UI que los datos han cambiado
                userData.setValue(currentUser);
            } else {
                errorMessage.setValue("Error al guardar la nueva foto en el perfil.");
            }
        });
    }
}