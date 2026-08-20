package br.com.gymapp.domain.repository
import br.com.gymapp.domain.model.*
interface GymRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun workouts(studentId: String): Result<List<Workout>>
    suspend fun startExecution(studentId: String, sessionId: String): Result<String>
    suspend fun registerSet(studentId: String, executionId: String, set: CompletedSet): Result<Unit>
    suspend fun finishExecution(studentId: String, executionId: String): Result<Unit>
}
