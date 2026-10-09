package br.com.ricardo.diariodeclasse.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import br.com.ricardo.diariodeclasse.notificacao.DestinoDaNotificacao
import br.com.ricardo.diariodeclasse.ui.afazer.AFazerScreen
import br.com.ricardo.diariodeclasse.ui.afazer.SecaoDoAFazer
import br.com.ricardo.diariodeclasse.ui.alunos.AlunoScreen
import br.com.ricardo.diariodeclasse.ui.chamada.ChamadaScreen
import br.com.ricardo.diariodeclasse.ui.configuracoes.ConfiguracoesScreen
import br.com.ricardo.diariodeclasse.ui.acompanhamento.AcompanhamentoScreen
import br.com.ricardo.diariodeclasse.ui.fotos.FotoScreen
import br.com.ricardo.diariodeclasse.ui.fotos.FotosScreen
import br.com.ricardo.diariodeclasse.ui.inicio.InicioScreen
import br.com.ricardo.diariodeclasse.ui.metas.FormularioMetaScreen
import br.com.ricardo.diariodeclasse.ui.metas.MetaScreen
import br.com.ricardo.diariodeclasse.ui.metricas.FormularioMetricaScreen
import br.com.ricardo.diariodeclasse.ui.metricas.MetricaScreen
import br.com.ricardo.diariodeclasse.ui.metricas.SondagemScreen
import br.com.ricardo.diariodeclasse.ui.turmas.FormularioTurmaScreen
import br.com.ricardo.diariodeclasse.ui.turmas.TurmaScreen

/**
 * Mapa de navegação do app. As telas não conhecem o `navController`:
 * recebem funções ("ao abrir turma", "ao voltar") e é aqui que se decide para onde ir.
 *
 * [destinoDaNotificacao] chega preenchido quando a professora toca numa notificação;
 * depois de navegar até ele, [aoAbrirDestinoDaNotificacao] o limpa.
 */
@Composable
fun AppNavHost(
    destinoDaNotificacao: DestinoDaNotificacao?,
    aoAbrirDestinoDaNotificacao: () -> Unit,
) {
    val navController: NavHostController = rememberNavController()
    val entradaAtual: NavBackStackEntry? = navController.currentBackStackEntryAsState().value
    val destinoAtual: NavDestination? = entradaAtual?.destination

    // Qual lista a aba "A fazer" mostra. Fica aqui, e não na tela, porque o Início
    // e as notificações também escolhem. `rememberSaveable` sobrevive a girar a tela.
    val secaoDoAFazer: MutableState<SecaoDoAFazer> = rememberSaveable {
        mutableStateOf(SecaoDoAFazer.PENDENCIAS_DOS_ALUNOS)
    }

    fun abrirAFazer(secao: SecaoDoAFazer) {
        secaoDoAFazer.value = secao
        abrirRaizDaAba(navController, AbaPrincipal.A_FAZER, AFazerRoute)
    }

    // As telas internas já têm o próprio Scaffold e tratam as bordas do sistema
    // (barra de status etc.). Por isso este Scaffold externo não aplica nenhuma
    // e só reserva o espaço da barra inferior.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (mostraBarraInferior(destinoAtual)) {
                BarraInferior(
                    destinoAtual = destinoAtual,
                    aoSelecionarAba = { aba -> navegarParaAba(navController, aba) },
                )
            }
        },
    ) { espacamentoDaBarraInferior: PaddingValues ->

        // `consumeWindowInsets` avisa às telas internas que a parte de baixo já foi
        // ocupada pela barra inferior; sem isso elas somariam de novo o espaço
        // da barra de navegação do sistema.
        val modifier = Modifier
            .padding(espacamentoDaBarraInferior)
            .consumeWindowInsets(espacamentoDaBarraInferior)

        NavHost(
            navController = navController,
            startDestination = InicioGrafo,
            modifier = modifier,
        ) {

            navigation<InicioGrafo>(startDestination = InicioRoute) {
                composable<InicioRoute> {
                    InicioScreen(
                        aoCadastrarTurma = { navController.navigate(FormularioTurmaRoute()) },
                        aoAbrirChamada = { turmaId, data ->
                            navController.navigate(ChamadaRoute(turmaId, data.toString()))
                        },
                        aoAbrirPendencias = { abrirAFazer(SecaoDoAFazer.PENDENCIAS_DOS_ALUNOS) },
                        aoAbrirLembretes = { abrirAFazer(SecaoDoAFazer.MEUS_LEMBRETES) },
                        aoAbrirConfiguracoes = { navController.navigate(ConfiguracoesRoute) },
                        // A aba Turma mostra a turma ativa, a mesma do Início.
                        aoAbrirTurma = { abrirRaizDaAba(navController, AbaPrincipal.TURMA, TurmaRoute) },
                    )
                }

                composable<ChamadaRoute> {
                    ChamadaScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }

                composable<ConfiguracoesRoute> {
                    ConfiguracoesScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }
            }

            navigation<AFazerGrafo>(startDestination = AFazerRoute) {
                composable<AFazerRoute> {
                    AFazerScreen(
                        secao = secaoDoAFazer.value,
                        aoTrocarSecao = { secao -> secaoDoAFazer.value = secao },
                        aoAbrirAluno = { alunoId -> navController.navigate(AlunoNoAFazerRoute(alunoId)) },
                    )
                }

                composable<AlunoNoAFazerRoute> {
                    AlunoScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }
            }

            navigation<TurmaGrafo>(startDestination = TurmaRoute) {
                composable<TurmaRoute> {
                    TurmaScreen(
                        aoCriarTurma = { navController.navigate(FormularioTurmaRoute()) },
                        aoEditarTurma = { turmaId -> navController.navigate(FormularioTurmaRoute(turmaId)) },
                        aoAbrirAluno = { alunoId -> navController.navigate(AlunoRoute(alunoId)) },
                    )
                }

                composable<FormularioTurmaRoute> {
                    FormularioTurmaScreen(
                        aoVoltar = { navController.popBackStack() },
                        aoExcluirTurma = { navController.popBackStack() },
                    )
                }

                composable<AlunoRoute> {
                    AlunoScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }
            }

            navigation<AcompanhamentoGrafo>(startDestination = AcompanhamentoRoute) {
                composable<AcompanhamentoRoute> {
                    AcompanhamentoScreen(
                        aoCriarMetrica = { turmaId -> navController.navigate(FormularioMetricaRoute(turmaId)) },
                        aoAbrirMetrica = { metricaId -> navController.navigate(MetricaRoute(metricaId)) },
                        aoCriarMeta = { turmaId -> navController.navigate(FormularioMetaRoute(turmaId)) },
                        aoAbrirMeta = { metaId -> navController.navigate(MetaRoute(metaId)) },
                        aoAbrirFotos = { turmaId -> navController.navigate(FotosRoute(turmaId)) },
                        aoAbrirFoto = { turmaId, fotoId -> navController.navigate(FotoRoute(turmaId, fotoId)) },
                    )
                }

                composable<FotosRoute> {
                    FotosScreen(
                        aoAbrirFoto = { turmaId, fotoId -> navController.navigate(FotoRoute(turmaId, fotoId)) },
                        aoVoltar = { navController.popBackStack() },
                    )
                }

                composable<FotoRoute> {
                    FotoScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }

                composable<FormularioMetricaRoute> {
                    FormularioMetricaScreen(
                        aoVoltar = { navController.popBackStack() },
                        // Fecha o formulário e a tela da métrica excluída de uma vez.
                        aoExcluirMetrica = {
                            navController.popBackStack(route = AcompanhamentoRoute, inclusive = false)
                        },
                    )
                }

                composable<MetricaRoute> {
                    MetricaScreen(
                        aoEditarMetrica = { turmaId, metricaId ->
                            navController.navigate(FormularioMetricaRoute(turmaId, metricaId))
                        },
                        aoAbrirSondagem = { metricaId, data ->
                            navController.navigate(SondagemRoute(metricaId, data.toString()))
                        },
                        aoVoltar = { navController.popBackStack() },
                    )
                }

                composable<SondagemRoute> {
                    SondagemScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }

                composable<FormularioMetaRoute> {
                    FormularioMetaScreen(
                        aoVoltar = { navController.popBackStack() },
                        aoExcluirMeta = {
                            navController.popBackStack(route = AcompanhamentoRoute, inclusive = false)
                        },
                    )
                }

                composable<MetaRoute> {
                    MetaScreen(
                        aoEditarMeta = { turmaId, metaId ->
                            navController.navigate(FormularioMetaRoute(turmaId, metaId))
                        },
                        aoAbrirSondagem = { metricaId, data ->
                            navController.navigate(SondagemRoute(metricaId, data.toString()))
                        },
                        aoAbrirAluno = { alunoId -> navController.navigate(AlunoNoAcompanhamentoRoute(alunoId)) },
                        aoVoltar = { navController.popBackStack() },
                    )
                }

                composable<AlunoNoAcompanhamentoRoute> {
                    AlunoScreen(
                        aoVoltar = { navController.popBackStack() },
                    )
                }
            }
        }
    }

    // Roda depois de o NavHost montar o mapa de telas, e de novo a cada destino novo.
    // A turma da notificação já foi escolhida como turma ativa pela MainActivity.
    LaunchedEffect(destinoDaNotificacao) {
        if (destinoDaNotificacao != null) {
            abrirAFazer(secaoDaNotificacao(destinoDaNotificacao))
            aoAbrirDestinoDaNotificacao()
        }
    }
}

private fun secaoDaNotificacao(destino: DestinoDaNotificacao): SecaoDoAFazer {
    return when (destino) {
        is DestinoDaNotificacao.PendenciasDaTurma -> SecaoDoAFazer.PENDENCIAS_DOS_ALUNOS
        is DestinoDaNotificacao.Lembretes -> SecaoDoAFazer.MEUS_LEMBRETES
    }
}

/**
 * Troca para a [aba] e fecha o que estava aberto nela, mostrando a tela [raiz].
 * Usado quando o pedido vem de fora da aba (Início, notificação): sem isso, ela
 * poderia reabrir numa tela antiga, como a de um aluno visto horas antes.
 */
private fun abrirRaizDaAba(navController: NavHostController, aba: AbaPrincipal, raiz: Any) {
    navegarParaAba(navController, aba)
    navController.popBackStack(route = raiz, inclusive = false)
}

/**
 * Em formulários (cadastro de turma, chamada, sondagem...) a barra some, para dar
 * espaço ao teclado e evitar sair no meio do preenchimento. Na foto em tela
 * cheia, some para a imagem ocupar mais espaço.
 */
private fun mostraBarraInferior(destinoAtual: NavDestination?): Boolean {
    // Antes de o NavHost montar a primeira tela, o destino ainda é nulo; o app sempre
    // começa no Início, então já mostramos a barra para ela não "piscar".
    if (destinoAtual == null) {
        return true
    }
    val estaNumFormulario: Boolean = destinoAtual.hasRoute(FormularioTurmaRoute::class) ||
        destinoAtual.hasRoute(FormularioMetricaRoute::class) ||
        destinoAtual.hasRoute(FormularioMetaRoute::class)
    val estaMarcandoAlunos: Boolean = destinoAtual.hasRoute(ChamadaRoute::class) ||
        destinoAtual.hasRoute(SondagemRoute::class)
    val estaVendoFoto: Boolean = destinoAtual.hasRoute(FotoRoute::class)
    if (estaNumFormulario || estaMarcandoAlunos || estaVendoFoto) {
        return false
    }
    return true
}

/**
 * Troca de aba guardando a pilha da aba atual e restaurando a da aba de destino.
 * Ao voltar a partir da raiz de qualquer aba, o usuário cai no Início antes de sair do app.
 */
private fun navegarParaAba(navController: NavHostController, aba: AbaPrincipal) {
    val telaInicialDoApp: NavDestination = navController.graph.findStartDestination()

    navController.navigate(aba.grafo) {
        popUpTo(telaInicialDoApp.id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
