package br.com.gymapp.domain.model

data class User(val id: String, val name: String, val email: String, val type: UserType = UserType.ALUNO)
enum class UserType { ALUNO, PROFESSOR }
data class Workout(val id: String, val name: String, val description: String?, val studentId: String, val sessions: List<WorkoutSession>) {
    val exerciseCount get() = sessions.sumOf { it.exercises.size }
    val estimatedMinutes get() = (exerciseCount * 8).coerceAtLeast(20)
}
data class WorkoutSession(val id: String, val name: String, val description: String?, val exercises: List<WorkoutExercise>)
data class WorkoutExercise(val id: String, val exerciseId: String, val name: String, val order: Int, val sets: Int, val repetitions: String, val suggestedLoad: String?, val restSeconds: Int, val note: String?)
data class CompletedSet(val exerciseId: String, val setNumber: Int, val repetitions: Int, val load: String, val completed: Boolean = true)
