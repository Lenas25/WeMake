package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utp.wemake.models.User;
import com.utp.wemake.repository.MemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddMembersViewModel extends ViewModel {

    private final MemberRepository memberRepository;
    private final MutableLiveData<List<Map<String, Object>>> _members = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> members = _members;
    private final MutableLiveData<Boolean> _updateSuccess = new MutableLiveData<>();
    public LiveData<Boolean> updateSuccess = _updateSuccess;
    private final MutableLiveData<List<User>> _searchResults = new MutableLiveData<>();
    public LiveData<List<User>> searchResults = _searchResults;
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    public AddMembersViewModel() {
        this.memberRepository = new MemberRepository();
        _updateSuccess.setValue(false);
        _isLoading.setValue(false);
    }

    public void loadMembers(String boardId) {
        _isLoading.setValue(true);
        memberRepository.getBoardMembers(boardId).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                _members.setValue(task.getResult());
            } else {
                _errorMessage.setValue("Error al cargar los miembros.");
            }
        });
    }

    public void updateMemberRole(String boardId, String userId, String newRole) {
        _isLoading.setValue(true);
        memberRepository.updateMemberRole(boardId, userId, newRole).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadMembers(boardId);
                _updateSuccess.setValue(true);
            } else {
                _errorMessage.setValue("Error al actualizar el rol.");
            }
        });
    }

    public void deleteMember(String boardId, String userId) {
        _isLoading.setValue(true);
        memberRepository.deleteMember(boardId, userId).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadMembers(boardId);
                _updateSuccess.setValue(true);
            } else {
                _errorMessage.setValue("Error al eliminar el miembro.");
            }
        });
    }

    public void searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            _searchResults.setValue(new ArrayList<>());
            return;
        }

        _isLoading.setValue(true);
        memberRepository.searchUsers(query).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                _searchResults.setValue(task.getResult());
            } else {
                // Opcional: puedes crear un LiveData de error de búsqueda
                _errorMessage.setValue("Error al buscar usuarios.");
            }
        });
    }

    public void addMemberToBoard(String boardId, String userId) {
        _isLoading.setValue(true);
        memberRepository.addMember(boardId, userId).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                // Si se añade con éxito, recargamos la lista de miembros actuales
                loadMembers(boardId);
                // Opcional: puedes limpiar los resultados de búsqueda
                _searchResults.setValue(new ArrayList<>());
                _updateSuccess.setValue(true);
            } else {
                _errorMessage.setValue("Error al añadir miembro.");
            }
        });
    }
}