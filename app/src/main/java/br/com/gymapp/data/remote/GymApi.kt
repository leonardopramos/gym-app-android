package br.com.gymapp.data.remote
import br.com.gymapp.domain.model.*
import br.com.gymapp.domain.repository.GymRepository
import retrofit2.http.*
interface GymApi {
    @POST("api/v1/auth/login") suspend fun login(@Body request: LoginRequest): LoginResponse
    @GET("api/v1/treinos/alunos/{studentId}") suspend fun workouts(@Header("Authorization") authorization: String, @Path("studentId") studentId: String): List<WorkoutResponse>
    @POST("api/v1/execucoes-treino") suspend fun start(@Header("Authorization") authorization: String, @Header("X-Aluno-Id") studentId: String, @Body request: StartRequest): ExecutionResponse
    @POST("api/v1/execucoes-treino/{id}/series") suspend fun register(@Header("Authorization") authorization: String, @Header("X-Aluno-Id") studentId: String, @Path("id") executionId: String, @Body request: SetRequest): ExecutionResponse
    @PUT("api/v1/execucoes-treino/{id}/finalizar") suspend fun finish(@Header("Authorization") authorization: String, @Header("X-Aluno-Id") studentId: String, @Path("id") executionId: String): ExecutionResponse
}
data class LoginRequest(val email: String, val senha: String)
data class LoginResponse(val token: String, val tipoToken: String, val usuario: UserResponse)
data class StartRequest(val sessaoTreinoId: String)
data class SetRequest(val sessaoExercicioId: String, val serie: Int, val repeticoesRealizadas: Int, val cargaUtilizada: Double?, val concluida: Boolean)
data class WorkoutResponse(val id: String, val nome: String, val descricao: String?, val alunoId: String, val professorId: String, val ativo: Boolean, val sessoes: List<SessionResponse>)
data class SessionResponse(val id: String, val nome: String, val descricao: String?, val ordem: Int, val exercicios: List<ExerciseResponse>)
data class ExerciseResponse(val id: String, val exercicioId: String, val exercicioNome: String, val ordem: Int, val series: Int?, val repeticoes: String?, val cargaSugerida: Double?, val descansoSegundos: Int?, val observacao: String?)
data class ExecutionResponse(val id: String)
data class UserResponse(val id: String, val nome: String, val email: String, val tipo: String, val ativo: Boolean)
class RetrofitGymRepository(private val api: GymApi) : GymRepository {
    private var accessToken: String? = null

    override suspend fun login(email: String, password: String) = runCatching {
        require(email.contains("@") && password.isNotBlank()) { "Informe e-mail e senha." }
        val response = api.login(LoginRequest(email.trim(), password))
        require(response.usuario.tipo == "ALUNO") { "Este aplicativo é exclusivo para alunos." }
        accessToken = response.token
        User(response.usuario.id, response.usuario.nome, response.usuario.email, UserType.ALUNO)
    }
    override suspend fun workouts(studentId: String) = runCatching { api.workouts(bearer(), studentId).map { it.toDomain() } }
    override suspend fun startExecution(studentId: String, sessionId: String) = runCatching { api.start(bearer(), studentId, StartRequest(sessionId)).id }
    override suspend fun registerSet(studentId: String, executionId: String, set: CompletedSet) = runCatching { api.register(bearer(), studentId, executionId, SetRequest(set.exerciseId, set.setNumber, set.repetitions, set.load.toDoubleOrNull(), set.completed)); Unit }
    override suspend fun finishExecution(studentId: String, executionId: String) = runCatching { api.finish(bearer(), studentId, executionId); Unit }

    private fun bearer(): String {
        val token = accessToken ?: error("Sessão expirada. Faça login novamente.")
        return "Bearer " + token
    }
}
private fun WorkoutResponse.toDomain() = Workout(id, nome, descricao, alunoId, sessoes.sortedBy { it.ordem }.map { it.toDomain() })
private fun SessionResponse.toDomain() = WorkoutSession(id, nome, descricao, exercicios.sortedBy { it.ordem }.map { item -> WorkoutExercise(item.id, item.exercicioId, item.exercicioNome, item.ordem, item.series ?: 1, item.repeticoes ?: "-", item.cargaSugerida?.toString(), item.descansoSegundos ?: 60, item.observacao) })
