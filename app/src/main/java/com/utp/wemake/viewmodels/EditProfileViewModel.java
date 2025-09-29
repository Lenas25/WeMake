package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.utp.wemake.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utp.wemake.repository.UserRepository;

public class EditProfileViewModel extends ViewModel {

    // 1. Referencias al Repositorio y a la Autenticación
    private final UserRepository userRepository = new UserRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // 2. LiveData para los DATOS que la VISTA observará

    // Guarda los datos del usuario. Es Mutable porque lo modificamos DENTRO de este ViewModel.
    private final MutableLiveData<User> userData = new MutableLiveData<>();

    // Notifica a la vista si la operación de guardado fue exitosa.
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();

    // Notifica a la vista si está ocurriendo una operación de carga.
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    // Constructor: se llama la primera vez que se crea el ViewModel
    public EditProfileViewModel() {
        loadCurrentUser();
    }

    // --- MÉTODOS PÚBLICOS (Getters para que la Vista observe los LiveData) ---

    public LiveData<User> getUserData() {
        return userData;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // --- LÓGICA INTERNA DEL VIEWMODEL ---

    /**
     * Carga los datos del usuario actual desde el repositorio.
     */
    private void loadCurrentUser() {
        isLoading.setValue(true); // Empieza la carga
        userRepository.getCurrentUserData().addOnCompleteListener(task -> {
            isLoading.setValue(false); // Termina la carga
            if (task.isSuccessful() && task.getResult() != null) {
                // Convierte el documento de Firestore a un objeto User
                User user = task.getResult().toObject(User.class);
                userData.setValue(user); // Publica los datos en el LiveData
            } else {
                userData.setValue(null);
            }
        });
    }

    /**
     * Guarda los datos actualizados del perfil.
     * @param updatedUser El objeto User con la nueva información del formulario.
     */
    public void saveProfile(User updatedUser) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            saveSuccess.setValue(false); // No se puede guardar si no hay usuario
            return;
        }

        // Asignamos el UID, que no viene del formulario pero es necesario.
        updatedUser.setUserid(currentUser.getUid());

        isLoading.setValue(true); // Empieza el guardado
        userRepository.updateProfileData(updatedUser).addOnCompleteListener(task -> {
            isLoading.setValue(false); // Termina el guardado
            // Notifica a la vista si la tarea fue exitosa o no.
            saveSuccess.setValue(task.isSuccessful());

            // Si fue exitoso, actualizamos los datos locales también.
            if (task.isSuccessful()) {
                userData.setValue(updatedUser);
            }
        });
    }
}