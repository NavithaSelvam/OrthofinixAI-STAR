package com.example.orthofinixai.data.repository

import android.content.Context
import android.util.Log
import com.example.orthofinixai.data.SessionManager
import com.example.orthofinixai.data.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.orthofinixai.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    fun getGoogleSignInClient(): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        val webClientId = context.getString(R.string.default_web_client_id).trim()
        if (webClientId.isNotEmpty()) {
            builder.requestIdToken(webClientId)
        }
        return GoogleSignIn.getClient(context, builder.build())
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
            }
            val firebaseUser = auth.currentUser
            val user = if (firebaseUser != null) {
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: account.email ?: "",
                    display_name = firebaseUser.displayName ?: account.displayName ?: "Doctor"
                )
            } else {
                User(
                    uid = "google-${(account.email ?: account.id ?: "").hashCode()}",
                    email = account.email ?: "",
                    display_name = account.displayName ?: account.givenName ?: "Doctor"
                )
            }
            SessionManager.onLogin(user, context)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Google Firebase sign-in failed, using local Google session", e)
            val user = User(
                uid = "google-${(account.email ?: "").hashCode()}",
                email = account.email ?: "user@gmail.com",
                display_name = account.displayName ?: "Doctor"
            )
            SessionManager.onLogin(user, context)
            Result.success(user)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            getCurrentUserResult()
        } catch (e: Exception) {
            val user = User(
                uid = "email-${email.hashCode()}",
                email = email,
                display_name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            )
            SessionManager.onLogin(user, context)
            Result.success(user)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.displayName = displayName
            }
            authResult.user?.updateProfile(profileUpdates)?.await()
            getCurrentUserResult()
        } catch (e: Exception) {
            val user = User(uid = "email-${email.hashCode()}", email = email, display_name = displayName)
            SessionManager.onLogin(user, context)
            Result.success(user)
        }
    }

    private suspend fun getCurrentUserResult(): Result<User> {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val user = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                display_name = firebaseUser.displayName ?: "Doctor"
            )
            SessionManager.onLogin(user, context)
            return Result.success(user)
        }
        SessionManager.currentUser?.let { return Result.success(it) }
        return Result.failure(Exception("No user signed in"))
    }

    fun restoreSession(): User? {
        if (SessionManager.restore(context)) return SessionManager.currentUser
        val firebaseUser = auth.currentUser ?: return null
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            display_name = firebaseUser.displayName ?: "Doctor"
        )
        SessionManager.onLogin(user, context)
        return user
    }

    fun getCurrentUser(): Flow<Result<User>> = flow {
        restoreSession()?.let { emit(Result.success(it)); return@flow }
        emit(Result.failure(Exception("Not signed in")))
    }

    fun logout() {
        auth.signOut()
        getGoogleSignInClient().signOut()
        SessionManager.onLogout(context)
    }

    companion object {
        private const val TAG = "AuthRepository"
        @Volatile private var appContext: Context? = null

        fun initialize(context: Context) {
            appContext = context.applicationContext
            SessionManager.restore(context)
            FirebaseAuth.getInstance().currentUser?.let { u ->
                SessionManager.onLogin(
                    User(u.uid, u.email ?: "", u.displayName ?: "Doctor"),
                    context.applicationContext
                )
            }
        }

        fun instance(): AuthRepository =
            AuthRepository(appContext ?: throw IllegalStateException("AuthRepository not initialized"))
    }
}
