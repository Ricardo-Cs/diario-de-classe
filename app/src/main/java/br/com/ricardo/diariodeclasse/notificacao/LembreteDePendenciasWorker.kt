package br.com.ricardo.diariodeclasse.notificacao

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.ricardo.diariodeclasse.data.local.entity.Lembrete
import br.com.ricardo.diariodeclasse.data.local.entity.PendenciaParaLembrete
import br.com.ricardo.diariodeclasse.data.repository.LembreteDiarioRepository
import br.com.ricardo.diariodeclasse.data.repository.LembreteRepository
import br.com.ricardo.diariodeclasse.data.repository.PendenciaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Aviso diário de pendências dos alunos e de lembretes da professora (duas
 * notificações separadas). Tarefa em segundo plano que roda de hora em hora, mesmo com o app fechado
 * ou depois de o celular reiniciar (o WorkManager guarda o agendamento).
 *
 * Por que de hora em hora, e não uma vez por dia às 7h? Para economizar bateria,
 * o Android pode atrasar tarefas agendadas; uma tarefa diária iria "escorregando"
 * de horário a cada dia. Rodando de hora em hora e avisando só uma vez por dia,
 * o aviso chega pouco depois das 7h. Cada verificação é uma consulta rápida no banco.
 *
 * `@HiltWorker` e `@AssistedInject`: o WorkManager é quem cria o worker e passa
 * `context` e `parametros` (`@Assisted`); o Hilt completa com os repositórios.
 */
@HiltWorker
class LembreteDePendenciasWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parametros: WorkerParameters,
    private val pendenciaRepository: PendenciaRepository,
    private val lembreteRepository: LembreteRepository,
    private val lembreteDiarioRepository: LembreteDiarioRepository,
    private val notificacao: NotificacaoDePendencias,
    private val notificacaoDeLembretes: NotificacaoDeLembretes,
    private val clock: Clock,
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val agora: LocalDateTime = LocalDateTime.now(clock)
        val ultimoDiaAvisado: LocalDate? = lembreteDiarioRepository.buscarUltimoDiaAvisado()
        if (!deveAvisarAgora(agora, ultimoDiaAvisado)) {
            return Result.success()
        }
        // Sem permissão, não marca o dia: na primeira abertura a verificação roda
        // enquanto a janela "Permitir notificações?" ainda está na tela, e o aviso
        // de hoje precisa sair na próxima verificação depois que ela permitir.
        if (!notificacao.temPermissao()) {
            return Result.success()
        }

        val hoje: LocalDate = agora.toLocalDate()
        val pendencias: List<PendenciaParaLembrete> = pendenciaRepository.buscarParaLembrete(hoje)
        notificacao.mostrar(pendencias)

        // Lembretes da professora: os de hoje e os atrasados, que voltam a cada dia útil até serem concluídos.
        val lembretes: List<Lembrete> = lembreteRepository.buscarParaNotificar(hoje)
        notificacaoDeLembretes.mostrar(lembretes, hoje)

        // Registra o dia mesmo sem pendências: o que for criado mais tarde,
        // a professora acabou de anotar e não precisa de aviso.
        lembreteDiarioRepository.registrarDiaAvisado(hoje)
        return Result.success()
    }

    companion object {
        private const val NOME_DA_TAREFA = "lembrete_de_pendencias"

        /**
         * `KEEP`: se a tarefa já estiver agendada, mantém a que existe. Assim dá para
         * chamar a cada abertura do app sem criar tarefas repetidas.
         */
        fun agendar(context: Context) {
            val tarefa: PeriodicWorkRequest = PeriodicWorkRequestBuilder<LembreteDePendenciasWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NOME_DA_TAREFA,
                ExistingPeriodicWorkPolicy.KEEP,
                tarefa,
            )
        }
    }
}
