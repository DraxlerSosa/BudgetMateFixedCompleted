package com.budgetmate.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.budgetmate.app.R
import com.budgetmate.app.databinding.ActivityAuthBinding
import com.budgetmate.app.ui.MainActivity
import com.budgetmate.app.util.snack

/**
 * Hosts both the Login and Register tabs on a single screen.
 * Uses ViewBinding — no findViewById calls anywhere.
 */
class AuthActivity : AppCompatActivity() {

    companion object { private const val TAG = "AuthActivity" }

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupSubmit()
        observeState()
    }

    private fun setupTabs() {
        binding.btnTabLogin.setOnClickListener    { switchTo(login = true) }
        binding.btnTabRegister.setOnClickListener { switchTo(login = false) }
        switchTo(login = true)
    }

    private fun switchTo(login: Boolean) {
        isLoginMode = login
        binding.btnTabLogin.isSelected    = login
        binding.btnTabRegister.isSelected = !login
        binding.tilDisplayName.visibility = if (login) View.GONE else View.VISIBLE
        binding.btnSubmit.text = getString(if (login) R.string.login else R.string.register)
        clearErrors()
        viewModel.reset()
    }

    private fun setupSubmit() {
        binding.btnSubmit.setOnClickListener {
            clearErrors()
            val u = binding.etUsername.text.toString()
            val p = binding.etPassword.text.toString()
            if (isLoginMode) viewModel.login(u, p)
            else viewModel.register(u, p, binding.etDisplayName.text.toString())
        }
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            val loading = state is AuthViewModel.AuthState.Loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !loading

            when (state) {
                is AuthViewModel.AuthState.Success -> {
                    Log.i(TAG, "Auth success → MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                AuthViewModel.AuthState.EmptyFields -> {
                    if (binding.etUsername.text.isNullOrBlank())
                        binding.tilUsername.error = getString(R.string.error_field_required)
                    if (binding.etPassword.text.isNullOrBlank())
                        binding.tilPassword.error = getString(R.string.error_field_required)
                }
                AuthViewModel.AuthState.InvalidCredentials ->
                    binding.tilPassword.error = getString(R.string.error_invalid_credentials)
                AuthViewModel.AuthState.UsernameTaken ->
                    binding.tilUsername.error = getString(R.string.error_username_taken)
                AuthViewModel.AuthState.PasswordTooShort ->
                    binding.tilPassword.error = getString(R.string.error_password_short)
                is AuthViewModel.AuthState.Error ->
                    binding.root.snack(state.message)
                else -> {}
            }
        }
    }

    private fun clearErrors() {
        binding.tilUsername.error    = null
        binding.tilPassword.error    = null
        binding.tilDisplayName.error = null
    }
}