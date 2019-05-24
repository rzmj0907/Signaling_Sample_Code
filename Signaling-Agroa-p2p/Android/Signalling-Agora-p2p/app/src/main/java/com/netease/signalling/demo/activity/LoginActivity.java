package com.netease.signalling.demo.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.signalling.demo.R;
import com.netease.signalling.demo.model.CacheInfo;
import com.netease.signalling.demo.utils.BaseUtil;
import com.netease.signalling.demo.utils.ToastHelper;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String ACCOUNT = "Account";

    private EditText edtLoginUserAccount;

    private EditText edtLoginPassword;

    private TextView loginTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findView();
        setViewsListener();
        setupViews();
    }

    private void findView() {
        edtLoginUserAccount = findViewById(R.id.edt_login_user_account);
        edtLoginPassword = findViewById(R.id.edt_login_user_password);
        loginTv = findViewById(R.id.tv_login);
    }

    TextWatcher watcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        @Override
        public void afterTextChanged(Editable s) {
            updateLoginEnable();
        }
    };

    private void updateLoginEnable() {
        String account = edtLoginUserAccount.getText().toString().trim();
        String password = edtLoginPassword.getText().toString().trim();
        loginTv.setEnabled(!TextUtils.isEmpty(account) && !TextUtils.isEmpty(password));
    }

    private void setViewsListener() {
        loginTv.setOnClickListener(this);
        edtLoginUserAccount.addTextChangedListener(watcher);
        edtLoginPassword.addTextChangedListener(watcher);
    }

    private void setupViews() {
        String account = getAccount();
        edtLoginUserAccount.setText(account);
    }

    private String getAccount() {
        SharedPreferences demo = getSP();
        return demo.getString(ACCOUNT, "");
    }

    private SharedPreferences getSP() {
        return getSharedPreferences("Demo", Context.MODE_PRIVATE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_login) {
            String account = edtLoginUserAccount.getText().toString();
            String password = edtLoginPassword.getText().toString();
            login(account, password);
        }
    }

    private boolean isInLogin;

    private void login(final String account, String password) {
        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            ToastHelper.showToast(this, "相关信息不能为空");
            return;
        }
        if (isInLogin) {
            ToastHelper.showToast(this, "正在登录中，请勿重复提交");
            return;
        }
        // 云信只提供消息通道，并不包含用户资料逻辑。开发者需要在管理后台或通过服务器接口将用户帐号和token同步到云信服务器。
        // 在这里直接使用同步到云信服务器的帐号和token登录。
        // 这里为了简便起见，demo就直接使用了密码的md5作为token。
        // 如果开发者直接使用这个demo，只更改appkey，然后就登入自己的账户体系的话，需要传入同步到云信服务器的token，而不是用户密码。
        String appKey = CacheInfo.getAppKey();
        boolean isDemo = appKey.contains("0c8e8a7")|| appKey.contains("8409b18a");
        password = isDemo ? BaseUtil.getStringMD5(password) : password;
        isInLogin = true;
        NIMClient.getService(AuthService.class).login(new LoginInfo(account, password)).setCallback(
                new RequestCallback<LoginInfo>() {

                    @Override
                    public void onSuccess(LoginInfo o) {
                        isInLogin = false;
                        saveAccount(account);
                        CacheInfo.setAccount(account);
                        ToastHelper.showToast(LoginActivity.this, "登录成功");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailed(int code) {
                        isInLogin = false;
                        ToastHelper.showToast(LoginActivity.this, "登录失败，code = " + code);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        isInLogin = false;
                        ToastHelper.showToast(LoginActivity.this, "登录异常");
                    }
                });
    }

    private void saveAccount(String account) {
        SharedPreferences sp = getSP();
        if (sp == null) {
            return;
        }
        SharedPreferences.Editor edit = sp.edit();
        edit.putString(ACCOUNT, account);
        edit.apply();
    }

    @Override
    protected void onDestroy() {
        if (edtLoginUserAccount != null) {
            edtLoginUserAccount.removeTextChangedListener(watcher);
        }
        if (edtLoginPassword != null) {
            edtLoginPassword.removeTextChangedListener(watcher);
        }
        super.onDestroy();
    }
}
