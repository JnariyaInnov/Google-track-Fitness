package com.blogspot.athletio;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.Firebase;

import org.json.JSONException;
import org.json.JSONObject;

/*
    From this class, the user of Online Chat can register for a new account

    Reference: https://firebase.google.com/docs/database/admin/save-data

 */

public class OnlineChatRegister extends AppCompatActivity {
    EditText username, password;
    Button registerButton;
    String user, pass;
    TextView login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_chat_register);

        username = (EditText) findViewById(R.id.online_chat_username);
        password = (EditText) findViewById(R.id.online_chat_password);
        registerButton = (Button) findViewById(R.id.registerButton);
        login = (TextView) findViewById(R.id.login);

        Firebase.setAndroidContext(this);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OnlineChatRegister.this, OnlineChatLogin.class));
                finish();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user = username.getText().toString();
                pass = password.getText().toString();

                if (user.equals("")) {
                    username.setError("can't be blank");
                } else if (pass.equals("")) {
                    password.setError("can't be blank");
                } else if (!user.matches("[A-Za-z0-9]+")) {
                    username.setError("only alphabet or number allowed");
                } else if (user.length() < 5) {
                    username.setError("at least 5 characters long");
                } else if (pass.length() < 5) {
                    password.setError("at least 5 characters long");
                } else {
                    final ProgressDialog pd = new ProgressDialog(OnlineChatRegister.this);
                    pd.setMessage("Loading...");
                    pd.show();

                    String url = "https://doctor-d6368.firebaseio.com/users.json";

                    StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String s) {
                            Firebase reference = new Firebase("https://doctor-d6368.firebaseio.com/users");

                            if (s.equals("null")) {
                                reference.child(user).child("password").setValue(pass);
                                Toast.makeText(OnlineChatRegister.this, "registration successful", Toast.LENGTH_LONG).show();
                            } else {
                                try {
                                    JSONObject obj = new JSONObject(s);

                                    if (!obj.has(user)) {
                                        reference.child(user).child("password").setValue(pass);
                                        Toast.makeText(OnlineChatRegister.this, "registration successful", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(OnlineChatRegister.this, "username already exists", Toast.LENGTH_LONG).show();
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            pd.dismiss();
                        }

                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            System.out.println("" + volleyError);
                            pd.dismiss();
                        }
                    });

                    RequestQueue rQueue = Volley.newRequestQueue(OnlineChatRegister.this);
                    rQueue.add(request);
                }
            }
        });
    }
}

