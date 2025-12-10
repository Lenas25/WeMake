package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utp.wemake.models.User;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.utils.EmailService;

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

    private final MutableLiveData<String> _emailStatus = new MutableLiveData<>();
    public LiveData<String> emailStatus = _emailStatus;

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
                _errorMessage.setValue("Error al buscar usuarios.");
            }
        });
    }

    public void addMemberToBoard(String boardId, User user) {
        _isLoading.setValue(true);

        memberRepository.addMember(boardId, user, new EmailService.EmailCallback() {
            @Override
            public void onSuccess() {
                _emailStatus.postValue("Miembro agregado y correo de invitación enviado.");
            }

            @Override
            public void onFailure(String errorMessage) {
                _emailStatus.postValue("Miembro agregado, pero falló el envío del correo: " + errorMessage);
            }
        }).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                loadMembers(boardId);
                _searchResults.setValue(new ArrayList<>());
                _updateSuccess.setValue(true);
            } else {
                _errorMessage.setValue("Error al añadir miembro en la base de datos.");
            }
        });
    }
}