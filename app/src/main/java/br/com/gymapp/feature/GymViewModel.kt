package br.com.gymapp.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gymapp.data.remote.NetworkFactory
import br.com.gymapp.data.remote.RetrofitGymRepository
import br.com.gymapp.domain.model.*
import br.com.gymapp.domain.repository.GymRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppScreen { data object Login : AppScreen; data object Home : AppScreen; data object Details : AppScreen; data object Session : AppScreen; data object Summary : AppScreen }
data class GymUiState(
    val screen: AppScreen = AppScreen.Login, val loading: Boolean = false, val error: String? = null,
    val user: User? = null, val workouts: List<Workout> = emptyList(), val selectedWorkout: Workout? = null,
    val executionId: String? = null, val startedAtMillis: Long? = null, val elapsedSeconds: Long = 0,
    val exerciseIndex: Int = 0, val setNumber: Int = 1, val completedSets: Int = 0, val totalSets: Int = 0,
    val restUntilMillis: Long? = null, val restRemainingSeconds: Long = 0
)

object WorkoutClock {
    fun elapsed(startedAt: Long, now: Long): Long = ((now - startedAt) / 1000).coerceAtLeast(0)
    fun remaining(until: Long?, now: Long): Long = until?.let { ((it - now) / 1000).coerceAtLeast(0) } ?: 0
}

class GymViewModel(private val repository: GymRepository = RetrofitGymRepository(NetworkFactory.createApi())) : ViewModel() {
    private val _state = MutableStateFlow(GymUiState())
    val state: StateFlow<GymUiState> = _state.asStateFlow()
    init { viewModelScope.launch { while (true) { delay(500); tick() } } }
    fun login(email: String, password: String) { if (_state.value.loading) return; viewModelScope.launch { update { it.copy(loading = true, error = null) }; repository.login(email, password).onSuccess { user -> update { it.copy(loading = false, user = user, screen = AppScreen.Home) }; loadWorkouts(user.id) }.onFailure { failure -> update { it.copy(loading = false, error = failure.message ?: "Não foi possível entrar.") } } } }
    private fun loadWorkouts(studentId: String) { viewModelScope.launch { repository.workouts(studentId).onSuccess { list -> update { it.copy(workouts = list, error = if (list.isEmpty()) "Nenhum treino encontrado para este aluno." else null) } }.onFailure { failure -> update { it.copy(error = failure.message ?: "Não foi possível carregar os treinos do backend.") } } } }
    fun select(workout: Workout) = update { it.copy(selectedWorkout = workout, screen = AppScreen.Details, error = null) }
    fun startWorkout() { val workout = _state.value.selectedWorkout ?: return; val session = workout.sessions.firstOrNull() ?: return; viewModelScope.launch { update { it.copy(loading = true, error = null) }; repository.startExecution(_state.value.user?.id.orEmpty(), session.id).onSuccess { id -> update { it.copy(loading = false, screen = AppScreen.Session, executionId = id, startedAtMillis = System.currentTimeMillis(), exerciseIndex = 0, setNumber = 1, completedSets = 0, totalSets = session.exercises.sumOf { e -> e.sets }, restUntilMillis = null) } }.onFailure { failure -> update { it.copy(loading = false, error = failure.message) } } } }
    fun completeSet() { val s = _state.value; val exercise = currentExercise(s) ?: return; if (s.restRemainingSeconds > 0) return; viewModelScope.launch { repository.registerSet(s.user?.id.orEmpty(), s.executionId.orEmpty(), CompletedSet(exercise.id, s.setNumber, exercise.repetitions.substringBefore("–").toIntOrNull() ?: 0, exercise.suggestedLoad.orEmpty())); val lastSet = s.setNumber >= exercise.sets; val lastExercise = s.exerciseIndex >= (s.selectedWorkout?.sessions?.firstOrNull()?.exercises?.lastIndex ?: 0); if (lastSet && lastExercise) { repository.finishExecution(s.user?.id.orEmpty(), s.executionId.orEmpty()); update { it.copy(screen = AppScreen.Summary, completedSets = it.completedSets + 1, elapsedSeconds = WorkoutClock.elapsed(it.startedAtMillis ?: System.currentTimeMillis(), System.currentTimeMillis())) } } else if (lastSet) update { it.copy(exerciseIndex = it.exerciseIndex + 1, setNumber = 1, completedSets = it.completedSets + 1, restUntilMillis = System.currentTimeMillis() + exercise.restSeconds * 1000L) } else update { it.copy(setNumber = it.setNumber + 1, completedSets = it.completedSets + 1, restUntilMillis = System.currentTimeMillis() + exercise.restSeconds * 1000L) } } }
    fun addRest() = update { it.copy(restUntilMillis = (it.restUntilMillis ?: System.currentTimeMillis()) + 30_000) }
    fun skipRest() = update { it.copy(restUntilMillis = null, restRemainingSeconds = 0) }
    fun finish() { val s = _state.value; viewModelScope.launch { repository.finishExecution(s.user?.id.orEmpty(), s.executionId.orEmpty()); update { it.copy(screen = AppScreen.Summary, elapsedSeconds = WorkoutClock.elapsed(it.startedAtMillis ?: System.currentTimeMillis(), System.currentTimeMillis())) } } }
    fun home() = update { it.copy(screen = AppScreen.Home, selectedWorkout = null, executionId = null, error = null) }
    private fun currentExercise(s: GymUiState) = s.selectedWorkout?.sessions?.firstOrNull()?.exercises?.getOrNull(s.exerciseIndex)
    private fun tick() { val s = _state.value; val started = s.startedAtMillis ?: return; val now = System.currentTimeMillis(); if (s.screen == AppScreen.Session) update { it.copy(elapsedSeconds = WorkoutClock.elapsed(started, now), restRemainingSeconds = WorkoutClock.remaining(it.restUntilMillis, now), restUntilMillis = if (WorkoutClock.remaining(it.restUntilMillis, now) == 0L) null else it.restUntilMillis) } }
    private fun update(block: (GymUiState) -> GymUiState) { _state.value = block(_state.value) }
}
