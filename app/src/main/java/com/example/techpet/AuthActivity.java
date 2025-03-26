package com.example.techpet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthActivity extends AppCompatActivity {

    private EditText usuarioText;
    private EditText contraseniaText;
    private Button iniciarButton;
    private ImageView googleSignInButton;

    private RequestQueue requestQueue;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        usuarioText = findViewById(R.id.usuarioText);
        contraseniaText = findViewById(R.id.contraseniaText);
        iniciarButton = findViewById(R.id.iniciarButton);
        googleSignInButton = findViewById(R.id.googleView);

        requestQueue = Volley.newRequestQueue(this);


        mAuth = FirebaseAuth.getInstance();


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        iniciarButton.setOnClickListener(v -> {
            String usuario = usuarioText.getText().toString();
            String contrasenia = contraseniaText.getText().toString();
            loginUser(usuario, contrasenia);
        });


        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            irAHome();
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {

                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {

                Log.w("AuthActivity", "Google sign in failed", e);
                Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {

                        Log.w("AuthActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(AuthActivity.this, "Error de autenticación", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUser(String usuario, String contrasenia) {

        irAHome();
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {

            irAHome();
        }
    }

    private void irAHome() {
        Intent intent = new Intent(AuthActivity.this, UserActivity.class);
        startActivity(intent);
        finish();
    }
}