package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.ImageRepository;
import com.utp.wemake.repository.UserRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {


    private final UserRepository userRepository = new UserRepository();
    private final ImageRepository imageRepository = new ImageRepository();

    // LiveData para los datos del usuario que la vista observará
    private final MutableLiveData<User> userData = new MutableLiveData<>();
    // LiveData para el estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    // LiveData para mensajes de error
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public HomeViewModel() {
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
}