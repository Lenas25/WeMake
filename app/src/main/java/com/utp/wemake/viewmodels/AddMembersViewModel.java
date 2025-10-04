package com.utp.wemake.viewmodels;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utp.wemake.MembersAdapter;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.BoardRepository;
import com.utp.wemake.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddMembersViewModel extends ViewModel {
    private final UserRepository userRepository = new UserRepository();
    private final BoardRepository boardRepository = new BoardRepository();

    // Fuentes de datos originales
    private final MutableLiveData<List<User>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Member>> memberDetailsMap = new MutableLiveData<>();
    private final MutableLiveData<Pair<User, Member>> selectedMemberData = new MutableLiveData<>();

    // LiveData combinado que la vista observará
    private final MediatorLiveData<List<MembersAdapter.MemberViewData>> displayList = new MediatorLiveData<>();

    // LiveData para el estado de la UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> closeScreen = new MutableLiveData<>(false);

    public LiveData<Pair<User, Member>> getSelectedMemberData() {
        return selectedMemberData;
    }
    private String currentBoardId;

    public AddMembersViewModel() {
        // El MediatorLiveData observa los cambios en las fuentes originales
        displayList.addSource(searchResults, users -> combineData());
        displayList.addSource(memberDetailsMap, details -> combineData());
    }

    // --- Getters para que la Activity los observe ---
    public LiveData<List<MembersAdapter.MemberViewData>> getDisplayList() { return displayList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> shouldCloseScreen() { return closeScreen; }

    public void init(String boardId) {
        if (this.currentBoardId == null) {
            this.currentBoardId = boardId;
            loadInitialMembers();
        }
    }

    private void loadInitialMembers() {
        isLoading.setValue(true);
        boardRepository.getMembersDetailsForBoard(currentBoardId).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                Map<String, Member> details = task.getResult();
                memberDetailsMap.setValue(details);
                // Ahora, obtenemos los detalles de los User para esos miembros
                if (!details.keySet().isEmpty()) {
                    userRepository.getUsersByIds(new ArrayList<>(details.keySet())).addOnCompleteListener(userTask -> {
                        if (userTask.isSuccessful()) {
                            searchResults.setValue(userTask.getResult());
                        }
                    });
                }
            } else {
                error.setValue("Error al cargar miembros.");
            }
        });
    }

    private void combineData() {
        List<User> users = searchResults.getValue();
        Map<String, Member> details = memberDetailsMap.getValue();
        if (users == null || details == null) return;

        List<MembersAdapter.MemberViewData> combinedList = new ArrayList<>();
        for (User user : users) {
            boolean isMember = details.containsKey(user.getUserid());
            Member memberDetail = isMember ? details.get(user.getUserid()) : null;
            combinedList.add(new MembersAdapter.MemberViewData(user, memberDetail, isMember));
        }
        displayList.setValue(combinedList);
    }

    public void searchUsers(String query) {
        if (query.trim().isEmpty()) {
            searchResults.setValue(new ArrayList<>());
            return;
        }
        isLoading.setValue(true);
        userRepository.searchUsersByEmail(query).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                searchResults.setValue(task.getResult());
            } else {
                error.setValue("Error en la búsqueda.");
            }
        });
    }

    public void addMember(User user) {
        isLoading.setValue(true);
        boardRepository.addMemberToBoard(currentBoardId, user.getUserid()).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadInitialMembers();
            } else {
                error.setValue("Error al añadir miembro.");
            }
        });
    }


    /**
     * Elimina un miembro y actualiza la lista local de IDs de miembros.
     * Este es el mismo método 'removeMember' que ya teníamos, solo nos aseguraremos
     * de que la Activity lo llame desde el callback del BottomSheet.
     */
    public void removeMember(User user) {
        isLoading.setValue(true);
        boardRepository.removeMemberFromBoard(currentBoardId, user.getUserid()).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadInitialMembers();
            } else {
                error.setValue("Error al eliminar miembro.");
            }
        });
    }

    public void saveAndClose() {
        closeScreen.setValue(true);
    }

    public void onMemberRoleClicked(User user) {
        isLoading.setValue(true);
        boardRepository.getMemberDetails(currentBoardId, user.getUserid()).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                Member memberDetails = task.getResult();
                selectedMemberData.setValue(new Pair<>(user, memberDetails));
            } else {
                error.setValue("No se pudo obtener el rol del miembro.");
            }
        });
    }

    /**
     * Actualiza el rol de un miembro y notifica a la UI si tiene éxito.
     */
    public void updateMemberRole(String userId, String newRole) {
        isLoading.setValue(true);
        boardRepository.updateMemberRole(currentBoardId, userId, newRole).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (!task.isSuccessful()) {
                error.setValue("Error al actualizar el rol.");
            }
        });
    }

    /**
     * Método de ayuda para eliminar por ID, que será llamado desde el BottomSheet.
     */
    public void removeMemberById(String userId) {
        // Buscamos el objeto User correspondiente en la lista de resultados
        List<User> currentResults = searchResults.getValue();
        if (currentResults != null) {
            for (User user : currentResults) {
                if (user.getUserid().equals(userId)) {
                    removeMember(user); // Llamamos al método principal
                    return;
                }
            }
        }
        // Si no se encuentra en los resultados de búsqueda, puede ser un miembro que no ha sido buscado.
        // En este caso, lo eliminamos y recargamos los miembros.
        isLoading.setValue(true);
        boardRepository.removeMemberFromBoard(currentBoardId, userId).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadInitialMembers(); // Recarga la lista de miembros para reflejar el cambio.
            } else {
                error.setValue("Error al eliminar miembro.");
            }
        });
    }
}