package com.utp.wemake.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Member;
import com.utp.wemake.repository.BoardRepository;
import com.utp.wemake.repository.MemberRepository;

public class SetupViewModel extends ViewModel {

    private final BoardRepository boardRepository = new BoardRepository();
    private final MemberRepository memberRepository = new MemberRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Boolean> joinSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Boolean> getJoinSuccess() { return joinSuccess; }
    public LiveData<String> getError() { return error; }

    public void joinBoardWithCode(String code) {
        if (code == null || code.trim().length() != 6) {
            error.setValue("El código debe tener 6 caracteres.");
            return;
        }

        // 1. Busca el tablero con el código
        boardRepository.findBoardByInviteCode(code.toUpperCase()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                // Tablero encontrado
                String boardId = task.getResult().getDocuments().get(0).getId();
                String currentUserId = auth.getUid();


            } else if (task.isSuccessful()) {
                // La búsqueda fue exitosa, pero no se encontraron resultados
                error.setValue("Código de invitación no válido.");
            } else {
                // La tarea de búsqueda falló
                error.setValue("Error al buscar el tablero.");
            }
        });
    }
}