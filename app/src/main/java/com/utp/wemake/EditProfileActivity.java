package com.utp.wemake;

import android.app.DatePickerDialog;
import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utp.wemake.databinding.ActivityEditProfileBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityEditProfileBinding binding;
    private TextInputEditText etFirstName, etLastName, etPublicName, etBirthDate, etEmail, etPhone;
    private TextInputLayout tilBirthDate;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        // Configurar toolbar personalizado
        setupToolbar();

        // Inicializar vistas
        initializeViews();

        // Configurar formato de fecha
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        selectedDate = Calendar.getInstance();

        // Configurar listeners
        setupListeners();

        // Cargar datos del usuario si existen
        loadUserData();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configurar el botón de navegación
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_edit_profile);
        }

        // Listener para el botón de navegación
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void initializeViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etPublicName = findViewById(R.id.et_public_name);
        etBirthDate = findViewById(R.id.et_birth_date);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        tilBirthDate = findViewById(R.id.til_birth_date);
    }

    private void setupListeners() {
        // Listener para el campo de fecha
        etBirthDate.setOnClickListener(v -> showDatePicker());
        // Listener para el ícono de calendario
        tilBirthDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    selectedDate.set(year1, month1, dayOfMonth);
                    String formattedDate = dateFormat.format(selectedDate.getTime());
                    etBirthDate.setText(formattedDate);
                },
                year, month, day
        );

        // Establecer fecha máxima (hoy)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Establecer fecha mínima (100 años atrás)
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void loadUserData() {
        // Aquí se cargarán los datos del usuario desde SharedPreferences, Base de datos.
        // etFirstName.setText(user.getFirstName());
        // etLastName.setText(user.getLastName());
    }

    private boolean validateFields() {
        boolean isValid = true;

        // Validar nombre
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("El nombre es requerido");
            isValid = false;
        }

        // Validar apellido
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("El apellido es requerido");
            isValid = false;
        }

        // Validar email
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError("El email es requerido");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido");
            isValid = false;
        }

        return isValid;
    }

    private void saveProfile() {
        if (validateFields()) {
            // Aquí se implementará la lógica para guardar los datos

            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String publicName = etPublicName.getText().toString().trim();
            String birthDate = etBirthDate.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            // Simular guardado
            Toast.makeText(this, "Perfil guardado exitosamente", Toast.LENGTH_SHORT).show();

            // Cerrar la actividad
            finish();
        } else {
            Snackbar.make(findViewById(android.R.id.content),
                    "Por favor, completa todos los campos requeridos",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save_profile) {
            saveProfile();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Opcional para mostrar diálogo de confirmación si hay cambios sin guardar
        super.onBackPressed();
    }
}