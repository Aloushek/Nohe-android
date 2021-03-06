package nohe.nohe_android.activity.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import nohe.nohe_android.R;
import nohe.nohe_android.activity.app.AppConfig;
import nohe.nohe_android.activity.interfaces.GetCurrentShipment;
import nohe.nohe_android.activity.interfaces.VolleyStringResponseListener;
import nohe.nohe_android.activity.models.UserModel;
import nohe.nohe_android.activity.services.LocationService;
import nohe.nohe_android.activity.services.LoginService;
import nohe.nohe_android.activity.services.ProgressDialogService;
import nohe.nohe_android.activity.services.RequestService;
import nohe.nohe_android.activity.services.ShipmentService;

public class LoginActivity extends AppCompatActivity {
    private Button loginBtn;
    private EditText usernameTb;
    private EditText passwordTb;
    private LoginService loginService;
    private ProgressDialogService progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameTb = (EditText) findViewById(R.id.userNameTb);
        passwordTb = (EditText) findViewById(R.id.passwordTb);
        loginBtn = (Button) findViewById(R.id.loginBtn);
        loginService = new LoginService(getApplicationContext());
        progressDialog = new ProgressDialogService(this);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String username = usernameTb.getText().toString().trim();
                String password = passwordTb.getText().toString().trim();

                if (!username.isEmpty() && checkPassword(password)) {
                    login(username, password);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please enter the credentials!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        runtimePermissions();
        checkIsUserLogged();
    }

    private void checkIsUserLogged() {
        try {
            if (loginService.isLoggedIn()) {
                checkShipment();
            }
        } catch (Exception ex) {
            loginService.logout();
        }
    }

    private boolean checkPassword(String password) {
        return password.length() >= 8 && Pattern.compile("[0-9]").matcher(password).find();
    }

    private void login(final String username, final String password) {
        progressDialog.showDialog(getString(R.string.loading));

        RequestService.makeJsonObjectRequest(Request.Method.POST, AppConfig.Urls.LOGIN, new VolleyStringResponseListener() {
            @Override
            public void onError(VolleyError message) {
                Toast.makeText(getApplicationContext(),
                        message.toString(), Toast.LENGTH_LONG).show();
                progressDialog.hideDialog();
            }

            @Override
            public void onResponse(String response) {
                progressDialog.hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    String token = jObj.getString("Token");

                    // Check for error node in json
                    if (!token.equals("")) {
                        // user successfully logged in
                        AppConfig.UserData.user = new UserModel(jObj.getJSONObject("User"));

                        // Create login session
                        loginService.login(token, AppConfig.UserData.user);

                        // Launch main activity
                        checkShipment();
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("Auth error");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public Map<String, String> getHeaders() {
                return new HashMap<String, String>();
            }

            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap();
                params.put("Username", username);
                params.put("Password", password);

                return params;
            }
        });
    }

    private void checkShipment() {
        progressDialog.showDialog(getString(R.string.loading));

        ShipmentService.getCurrentService(new GetCurrentShipment() {
            @Override
            public void onResponse() {
                progressDialog.hideDialog();
                openShipmentActivity();
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> header = new HashMap<String, String>();
                header.put("Token", loginService.getToken());
                return header;
            }
        });
    }

    private void openShipmentActivity() {
        Intent intent;

        if (AppConfig.ShipmentData.shipment == null) {
            intent = new Intent(LoginActivity.this, StartShipmentActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, ShipmentInProgressActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private boolean runtimePermissions() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission_group.LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, 100);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "You must allow location permissions", Toast.LENGTH_LONG).show();
                runtimePermissions();
            }
        }
    }
}
