package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.utp.wemake.dto.DashboardResponse;
import com.utp.wemake.services.ApiService;
import com.utp.wemake.services.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardViewModel extends ViewModel {

    private final ApiService apiService;

    private final MutableLiveData<DashboardResponse> _dashboardData = new MutableLiveData<>();
    public LiveData<DashboardResponse> getDashboardData() { return _dashboardData; }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> getIsLoading() { return _isLoading; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public DashboardViewModel() {
        this.apiService = RetrofitClient.getApiService();
    }

    public void loadDashboardData(String boardId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null); // Limpiar errores previos

        apiService.getDashboardData(boardId).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _dashboardData.setValue(response.body());
                } else {
                    _errorMessage.setValue("No se pudieron cargar los datos. Código: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Error de red: " + t.getMessage());
            }
        });
    }
}