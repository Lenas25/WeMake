package com.utp.wemake;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.models.User;
import com.utp.wemake.viewmodels.EditProfileViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etPublicName, etBirthDate, etEmail, etPhone;
    private Calendar selectedDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ViewModel para manejar la lógica de datos
    private EditProfileViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Inicializa el ViewModel
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);

        initializeViews();
        setupToolbar();
        setupListeners();
        setupObservers();
    }

    // --- MÉTODOS DE CONFIGURACIÓN ---
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        // El menú se infla automáticamente por app:menu en el XML
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                saveChanges();
                return true;
            }
            return false;
        });
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.et_fullname);
        etPublicName = findViewById(R.id.et_public_name);
        etBirthDate = findViewById(R.id.et_birth_date);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
    }

    private void setupListeners() {
        etBirthDate.setOnClickListener(v -> showDatePicker());
    }

    /**
     * Observa los cambios en el ViewModel (datos del usuario, resultado del guardado).
     */
    private void setupObservers() {
        // Observador para cargar los datos iniciales del usuario
        viewModel.getUserData().observe(this, user -> {
            if (user != null) {
                etFullName.setText(user.getName());
                etPublicName.setText(user.getPublicName());
                etEmail.setText(user.getEmail());
                etPhone.setText(user.getPhone());

                if (user.getBirthDate() != null && !user.getBirthDate().isEmpty()) {
                    etBirthDate.setText(user.getBirthDate());
                }
            }
        });

        // Observador para el resultado de la operación de guardado
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                finish(); // Cierra la pantalla y vuelve al ProfileFragment
            } else {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LÓGICA DE LA PANTALLA ---

    /**
     * Recoge los datos, los valida y llama al ViewModel para guardarlos.
     */
    private void saveChanges() {
        if (!validateFields()) {
            Toast.makeText(this, "Por favor, corrige los errores", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Obtiene la copia actual del usuario desde el ViewModel.
        //    Esto es importante para no perder datos que no están en el formulario (como photoUrl, coins, etc.)
        User currentUser = viewModel.getUserData().getValue();
        if (currentUser == null) {
            // Manejar el caso improbable de que los datos aún no se hayan cargado.
            Toast.makeText(this, "Datos del usuario no disponibles. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Actualiza solo los campos que se modificaron en el formulario.
        currentUser.setName(etFullName.getText().toString().trim());
        currentUser.setPublicName(etPublicName.getText().toString().trim());
        currentUser.setEmail(etEmail.getText().toString().trim());
        currentUser.setPhone(etPhone.getText().toString().trim());
        currentUser.setBirthDate(etBirthDate.getText().toString().trim());

        // 3. Llama al ViewModel para que guarde el objeto User completamente actualizado.
        viewModel.saveProfile(currentUser);
    }
    private boolean validateFields() {
        boolean isValid = true;
        // Validar nombre completo
        if (etFullName.getText().toString().trim().isEmpty()) {
            etFullName.setError("El nombre es requerido");
            isValid = false;
        } else {
            etFullName.setError(null); // Limpiar error
        }

        // Validar nombre público
        if (etPublicName.getText().toString().trim().isEmpty()) {
            etPublicName.setError("El nombre público es requerido");
            isValid = false;
        } else {
            etPublicName.setError(null); // Limpiar error
        }

        // Validar email
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError("El email es requerido");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido");
            isValid = false;
        } else {
            etEmail.setError(null); // Limpiar error
        }

        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            etPhone.setError("El teléfono es requerido");
            isValid = false;
        } else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            etPhone.setError("Número de teléfono inválido");
            isValid = false;
        } else {
            etPhone.setError(null); // Limpiar error
        }

        return isValid;
    }

    private void showDatePicker() {
        Calendar calendar = (selectedDate != null) ? selectedDate : Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    etBirthDate.setText(dateFormat.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}