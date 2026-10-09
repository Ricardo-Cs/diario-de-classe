# CLAUDE.md — Diário de Aula Virtual (nome provisório)

Contexto e decisões do projeto para orientar o trabalho neste repositório.

> Nome do app ainda **não definido** (candidatos até agora: Pastinha, Turma em Dia, Broto). O nome exibido fica em `app_name` (`strings.xml`) e pode mudar sem impacto. O `applicationId` e o `namespace` (`br.com.ricardo.diariodeclasse`) já definidos no `build.gradle.kts` **não devem ser alterados**.

---

## 1. Visão geral

App Android nativo para **organização da sala de aula** de turmas do ensino básico (educação infantil e anos iniciais). Pedido por uma pedagoga; descrito por ela como "uma espécie de diário de aula virtual".

### O problema
A professora usa hoje duas ferramentas:
1. **Sistema oficial da rede** — usado **única e exclusivamente para fazer a chamada**. Nada além disso é registrado lá.
2. **Diário de classe manual (papel)** — onde ela registra todo o resto: quem faltou, anotações específicas sobre alunos, atividades pendentes do aluno X etc.

**O objetivo do app é substituir esse diário manual.** Não substitui o sistema oficial (frequência legal) e não compete com ele: cobre o acompanhamento pedagógico do dia a dia que hoje só existe no papel.

Consequência para o design: tudo o que ela anota no caderno precisa ser **mais rápido de registrar no app do que no papel**, senão ela volta ao caderno.

### Desenvolvedor
- Vem de TypeScript (Angular, NestJS, React Native/Expo) e Java (Spring Boot).
- Está aprendendo Kotlin e o ecossistema Android com este projeto.
- Ao sugerir código, explique as decisões e os conceitos específicos de Android/Kotlin (ciclo de vida, Compose, coroutines/Flow, Room, WorkManager), fazendo paralelos com TS/Java quando ajudar.

### Regra: legibilidade do código
O desenvolvedor **nunca trabalhou com Kotlin** e precisa **entender e apresentar todo o código**. A prioridade é código legível por si só, não por comentários.
- Quando conciso e explícito entrarem em conflito, escolher o **explícito**, mesmo que seja menos idiomático.
- Evitar: encadeamento de scope functions (`let`, `also`, `apply`, `run`), referências de função (`::funcao`), `it` implícito em lambdas não triviais, operadores sobrecarregados pouco óbvios (`map += a to b`) e truques de inferência de tipo.
- Preferir: `if`/`when` e variáveis intermediárias com nomes claros; parâmetros de lambda nomeados; tipos declarados quando não forem óbvios pelo lado direito.
- Nomes descritivos em português; funções curtas, com uma responsabilidade.
- Recursos inevitáveis de Kotlin/Android (`suspend`, `Flow`, `@Composable`, anotações do Hilt/Room, `data class`, `sealed interface`) são usados normalmente, mas explicados na conversa quando aparecerem pela primeira vez.
- Comentários só para o "porquê" não óbvio, nunca para compensar código confuso.

---

## 2. Funcionalidades

| Funcionalidade | Descrição | Dificuldade |
|---|---|---|
| Turmas e alunos | Cadastro da turma e dos alunos | Baixa |
| Anotações por aluno | Texto com data (as "anotações específicas" do caderno) | Baixa |
| Registro de faltas | Quem faltou no dia, com observação; base para as pendências | Média |
| Pendências ("colocar na pasta") | Aluno faltou → atividades ficam pendentes para ele fazer depois | Média |
| Notificação | Se o aluno X não fez a atividade Y, notificar no dia seguinte | Média |
| Metas da turma | Meta **e forma de medição** definidas pela própria professora | Média |
| Métricas personalizáveis | Métricas definidas pela professora, com histórico | Média/alta |
| Fotos do dia | Registro do dia da turma em fotos, para histórico (pedido da professora após o uso) | Média |

### Diretrizes de design

**Navegação** (reorganizada em out/2026: pendências e lembretes não tinham acesso direto)
- Barra inferior: **Início · A fazer · Turma · Acompanhar**. Todas partem da mesma **turma ativa**, trocada pelo seletor no topo de cada aba.
- **A fazer**: pendências dos alunos e lembretes da professora, em duas seções. As notificações e os links do Início abrem direto aqui.
- **Turma**: alunos da turma ativa; "Nova turma" e editar ficam na própria tela (não há lista de turmas).
- **Acompanhar** (título "Acompanhamento"): fotos do dia, metas e métricas. O nome longo não cabe na barra.
- Botão "Anotar" no Início: escolhe o aluno numa lista e já abre o campo de texto (2 toques até escrever; antes eram 3 telas).
- Backup fica em Configurações (engrenagem no Início): é raro e não merece aba.

**Registro de faltas (chamada)**
- A chamada oficial continua no sistema da rede; no app, o que importa é **quem faltou**, porque é isso que ela anota no caderno e é daí que saem as pendências.
- Todos os alunos **presentes por padrão**; a professora toca só nos ausentes. Deve levar segundos.
- Um registro por dia/turma, **editável** depois.
- Observação opcional na falta (atestado, aviso da família).
- Ao marcar ausência, oferecer o registro das atividades pendentes daquele aluno.
- Ideias futuras: histórico de faltas por aluno/mês numa tela; alerta de faltas consecutivas (apoio à busca ativa).

**Pendências**
- É a funcionalidade que mais substitui o caderno: "atividades pendentes do aluno X".
- Deve ser possível criar pendência a partir de uma falta **ou** avulsa (aluno não terminou a atividade em sala, por exemplo).
- Visão rápida de "o que está pendente hoje" por turma e por aluno.

**Exemplo real da professora (out/2026)**
> "Temos 14 crianças alfabéticas e 11 silábicas com valor sonoro, temos até a primeira semana de novembro para desestabilizar e elas virarem alfabéticas."

Os níveis são os da psicogênese da língua escrita (pré-silábico, silábico sem valor sonoro, silábico com valor sonoro, silábico-alfabético, alfabético), registrados em **sondagens** periódicas. O exemplo orienta o desenho de Métricas e Metas abaixo.

**Métricas**
- Primeiro tipo a implementar: **escala de níveis nomeados e ordenados**, definidos pela professora. Cada aluno está em **um nível por vez**.
- Os níveis são registrados em **sondagens** (métrica + data), e o histórico mostra a evolução de cada aluno.
- Registrar uma sondagem funciona **como a chamada**: a turma inteira aparece com o último nível já preenchido e a professora toca só em quem mudou.
- Outros tipos (número, sim/não) entram depois, quando houver um caso real.

**Metas**
- A professora cria a meta **e escolhe como ela é medida**. O app não impõe critérios.
- Metas são **livres** e sempre sobre um grupo de alunos: descrição, prazo opcional e alunos (com atalho "toda a turma"). As metas ainda são vagas até para a professora, então o app não deve engessá-las.
- Duas formas de acompanhar, escolhidas na criação e fixas depois:
  - **marcando à mão** (padrão): ela marca quem atingiu (sim/não). Ex.: "conhecer a família numérica do 10 ao 80";
  - **pela métrica**: "estes alunos chegam ao nível Alfabético até 06/11", com progresso **calculado** a partir das sondagens.
- O que é sobre a própria professora (entregar portfólio, plano de ação) **não é meta**: é **lembrete**.

**Fotos do dia** (pedido da professora em out/2026, após usar a versão de teste)
- Fotos da **turma no dia** (não de um aluno), com legenda opcional. Sem vídeo: arquivos grandes demais para guardar e exportar sem backend.
- Câmera (app de câmera do celular, `TakePicture`) ou galeria (Photo Picker); nenhuma das duas pede permissão.
- Reduzidas ao entrar (lado maior 1600 px, JPEG 85, rotação do EXIF aplicada) e guardadas em `files/fotos/`; o banco guarda só o nome do arquivo.
- Ficam **fora do Auto Backup** (limite de 25 MB) e vão na exportação, que virou um `.zip` (JSON + fotos). Na transferência direta entre celulares elas vão junto.
- Na aba Acompanhamento, o card "Fotos do dia" vem antes das metas; a tela de fotos da turma mostra a linha do tempo por dia.

**Lembretes da professora**
- Descrição + data; gerais (não pertencem a uma turma).
- Aparecem no Início quando atrasados ou nos próximos 7 dias; a lista completa fica na aba "A fazer" (seção "Meus lembretes"), ao lado das pendências dos alunos.
- Notificação diária própria (canal separado das pendências) com os do dia e os atrasados, até serem concluídos.

**Grupos (fora do escopo por enquanto)**
- Estavam no pedido original como "grupos de acompanhamento", mas foram retirados em out/2026: métricas (níveis) e metas (conjunto de alunos com objetivo) já cobrem o que se esperava deles.
- Só voltam se a professora mostrar marcações reais que não sejam nível nem meta (ex.: "precisa de atenção").

**Tags nas anotações (fora do escopo por enquanto)**
- Retiradas em out/2026: escolher tags é um passo a mais no registro, que precisa ser mais rápido que o caderno. O histórico do aluno em ordem de data basta.
- Só voltam se a professora sentir falta de procurar anotações por tema (família, comportamento etc.).

**Notificações**
- Agendar com WorkManager (ou AlarmManager se precisar de horário exato).
- Atenção à permissão `POST_NOTIFICATIONS` (Android 13+), canais de notificação e restrições de bateria.

---

## 3. Stack e configuração

- **Linguagem:** Kotlin (Java descartado: Compose exige Kotlin e a documentação oficial é Kotlin-first).
- **UI:** Jetpack Compose (template "Empty Activity"; não usar Views/XML).
- **Arquitetura:** MVVM — ViewModel + `StateFlow`.
- **Persistência:** Room (SQLite).
- **Tarefas em segundo plano:** WorkManager.
- **Injeção de dependência:** Hilt.
- **Assíncrono:** Coroutines e Flow.
- **Min SDK:** 26 (Android 8.0) — canais de notificação e `java.time` sem desugaring.
- **Build:** Gradle com Kotlin DSL (`build.gradle.kts`).
- **Ambiente:** Zorin OS (Ubuntu). Emulador requer usuário no grupo `kvm`.

---

## 4. Backend: decisão

**MVP sem backend, 100% local.** Motivos: curva de aprendizado já grande, necessidade de validar o modelo com a professora antes, sincronização offline é complexa, e dados só no aparelho simplificam a LGPD.

Backend futuro (provavelmente **NestJS + PostgreSQL**) quando houver: uso em mais de um aparelho, acesso da coordenação/outras professoras, contas compartilhadas, ou exigência externa. O app passará a ser **offline-first**: Room continua sendo a fonte da verdade na UI e o servidor é o destino da sincronização.

### Regras obrigatórias desde já (preparação para o backend)
1. **Repository pattern:** ViewModels acessam repositórios (`AlunoRepository` etc.), **nunca** DAOs diretamente.
2. **IDs em UUID** (String), não autoincremento.
3. **`createdAt` e `updatedAt`** em todas as entidades.
4. **Soft delete** com `deletedAt` em vez de apagar linhas; consultas padrão filtram registros excluídos.

### Mitigação de perda de dados no MVP
Como o app substitui o caderno, perder o aparelho não pode significar perder o diário.
- Configurar **Auto Backup** do Android (banco na conta Google).
- Funcionalidade de **exportar/importar** dados (`.zip` com o JSON e as fotos; o `.json` antigo continua sendo importado) e, possivelmente, relatório em PDF.

---

## 5. Modelo de dados (rascunho)

- `Turma` — nome, ano/série, período, ano letivo
- `Aluno` — nome, turma, dados mínimos necessários
- `Chamada` — turma + data (única por turma/dia)
- `RegistroPresenca` — chamada + aluno + presente/ausente + observação
- `Pendencia` — aluno + atividade + vínculo **opcional** com a ausência + status (pendente/entregue) + data prevista de lembrete
- `Metrica` — turma + nome (ex.: "Nível de escrita")
- `NivelDaMetrica` — métrica + nome + ordem (os degraus da escala)
- `Sondagem` — métrica + data (única por métrica/dia, como a `Chamada`)
- `ResultadoDaSondagem` — sondagem + aluno + nível (único por sondagem/aluno)
- `Meta` — turma + descrição + prazo opcional + métrica e nível-alvo opcionais (sem eles, a meta é marcada à mão); progresso não é armazenado
- `AlunoNaMeta` — alunos que a meta acompanha (N:N), com nível inicial e "atingiu em" (metas à mão)
- `Anotacao` — aluno + texto + data
- `Lembrete` — descrição + data + concluído em (da professora, sem turma)
- `Foto` — turma + data + nome do arquivo + legenda opcional

Todas as entidades seguem as regras da seção 4 (UUID, timestamps, soft delete).

---

## 6. Privacidade (LGPD)

- Dados de crianças: tratamento com cuidado especial (consentimento dos responsáveis, Art. 14).
- Coletar **apenas o mínimo necessário** de cada aluno.
- Manter tudo local no MVP; não enviar dados a serviços externos sem decisão explícita.

---

## 7. Contexto de mercado

Já existem vários "diários de classe digitais" (apps de secretarias estaduais, MSTECH, Tecsystem, Diário Fácil etc.), mas focados na parte **burocrática/oficial** (frequência, notas, conteúdos). ClassDojo e Google Classroom cobrem comunicação e entrega de atividades.

**Diferencial deste app:** substituir o **caderno pessoal da professora** — o acompanhamento pedagógico cotidiano que os sistemas oficiais não cobrem: faltas com observações, pendências por aluno, lembretes, anotações, metas e registros de evolução, adequados a etapas em que não há avaliação por nota.

---

## 8. Decisões com a professora e próximos passos

**Respondido**
- *Por que anota no papel?* O sistema oficial serve só para a chamada. Todo o resto (faltas, anotações específicas, pendências) vai para um diário manual, que é o que o app deve substituir.
- *Como as metas são medidas?* A própria professora define as metas e a forma de medição.
- *Nome do app:* ainda não definido.
- *Que métricas ela usaria?* Parcialmente respondido: níveis de escrita (psicogênese), acompanhados por sondagens, com meta de avanço até a primeira semana de novembro de 2026 (ver seção 2).

**Em aberto**
- Há outras métricas além dos níveis de escrita (ex.: matemática, leitura)?
- Com que frequência ela faz as sondagens?
- Nome do app.

**Desenvolvimento**
1. ~~Dependências: Room, WorkManager, Hilt, Navigation Compose.~~
2. ~~Estrutura de pacotes MVVM.~~
3. ~~Turmas e Alunos.~~
4. ~~Registro de faltas → Pendências → Notificações.~~
5. ~~Anotações~~ → ~~Métricas (escala de níveis) e Metas ligadas a métricas~~ (aba Acompanhamento, que se chamava Diário até out/2026). Grupos e tags nas anotações foram retirados do plano em out/2026 (ver seção 2); próximos passos saem do uso real da professora. Métricas vieram antes porque a meta real da professora tem prazo no início de novembro de 2026.
6. ~~Backup/exportação JSON~~ (estender a cada entidade nova); relatório em PDF em aberto.
7. Pedidos da professora após o uso: ~~fotos do dia~~ → aba de perfil (conteúdo ainda a definir com ela).
