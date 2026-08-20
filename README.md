# Gym App Android

Primeira versão funcional do app Android para aluno, construída sobre o domínio existente em `../gym-app-backend`.

## Executar

O login usa `POST /api/v1/auth/login` e as chamadas seguintes usam o token JWT retornado pelo backend.

O login usa `POST /api/v1/auth/login` e as chamadas seguintes usam o token JWT retornado pelo backend.

Abra o diretório no Android Studio e execute a variante `debug` em um emulador ou dispositivo Android. A tela de login usa o endpoint JWT do backend; informe as credenciais de um aluno cadastrado.

Comandos úteis:

```bash
./gradlew test
./gradlew assembleDebug
```

## Arquitetura

A aplicação usa Compose + Material 3 e um fluxo unidirecional simples:

`Compose UI -> GymViewModel -> GymRepository -> RetrofitGymRepository -> Backend Spring`

Os modelos em `domain/model` são independentes dos DTOs HTTP. `data/remote/GymApi.kt` contém os DTOs e a implementação Retrofit baseada nos contratos atuais do backend.

O ViewModel mantém `GymUiState` explícito e concentra transições do fluxo: login, seleção, início, conclusão de série, descanso e finalização. Os Composables renderizam o estado e emitem eventos.

### Timer

O cronômetro do treino guarda `startedAtMillis`; o descanso guarda `restUntilMillis`. O valor exibido é recalculado a partir de `System.currentTimeMillis()`, e não por decremento de uma variável na UI. Assim Compose, rotação e retorno do background não reiniciam o relógio enquanto o processo da aplicação estiver vivo. `WorkoutClock` é puro e possui testes unitários.

## Contrato do backend usado

O domínio foi conferido em `../gym-app-backend`:

- `GET /api/v1/treinos/alunos/{alunoId}` lista `TreinoResponse` com sessões e exercícios.
- `POST /api/v1/execucoes-treino` inicia uma execução com `{ "sessaoTreinoId": "..." }` e header `X-Aluno-Id`.
- `POST /api/v1/execucoes-treino/{id}/series` registra uma série com `sessaoExercicioId`, série, repetições, carga e concluída.
- `PUT /api/v1/execucoes-treino/{id}/finalizar` finaliza a execução com `X-Aluno-Id`.

O backend atual não tem endpoint de login ou validação de senha. O app envia e-mail e senha ao endpoint de login, guarda o JWT em memória e envia `Authorization: Bearer <token>` nos endpoints protegidos de treinos e execuções. Nenhum treino é criado pelo app; se o aluno não tiver treinos, a tela informa isso.

A API já está ativa no `GymViewModel` por meio de `RetrofitGymRepository`. A URL padrão é `http://10.0.2.2:8080/` para o emulador Android. A API local normalmente precisa de `10.0.2.2` no emulador Android, além de configuração de cleartext apenas para desenvolvimento.

## Estrutura

- `domain/model`: modelos usados pela aplicação.
- `domain/repository`: contrato consumido pelo ViewModel.
- `data/remote`: autenticação JWT, acesso aos endpoints reais e mapeamento dos DTOs.
- `feature/GymViewModel.kt`: estado e regras do fluxo do aluno.
- `MainActivity.kt`: destinos Compose da primeira versão.

## Android concepts for a backend Java developer

| Backend Java/Spring | Android | Analogía conceitual |
| --- | --- | --- |
| REST Controller | Composable / destino de navegação | Entrada e saída da interação, não uma equivalência estrutural |
| Service | ViewModel ou Use Case | Estado e regras próximas à feature |
| Repository | Repository | Abstrai a origem dos dados |
| DTO | Network DTO | Formato de transporte HTTP |
| Dependency Injection | Hilt ou composição manual | Fornece dependências sem acoplar telas |
| HTTP client | Retrofit | Cliente declarativo da API |
| Estado da request | UiState / StateFlow | Estado observável e renderizável |
| application.properties | BuildConfig/configuração | Configuração por ambiente |

As analogias ajudam a localizar responsabilidades, mas Android não é uma cópia da arquitetura Spring. A UI é reativa, o lifecycle é variável e o ViewModel deve sobreviver a mudanças de configuração.

## Nova feature

Crie o modelo de domínio necessário, adicione métodos ao `GymRepository`, implemente fake e remoto, exponha estado/eventos no ViewModel e só então crie os Composables. Mantenha DTOs HTTP fora da UI e cubra regras temporais ou de transição com testes unitários.
