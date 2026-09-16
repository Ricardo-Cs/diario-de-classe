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

---

## 2. Funcionalidades

| Funcionalidade | Descrição | Dificuldade |
|---|---|---|
| Turmas e alunos | Cadastro da turma e dos alunos | Baixa |
| Anotações por aluno | Texto com data, possivelmente com tags (as "anotações específicas" do caderno) | Baixa |
| Registro de faltas | Quem faltou no dia, com observação; base para as pendências | Média |
| Pendências ("colocar na pasta") | Aluno faltou → atividades ficam pendentes para ele fazer depois | Média |
| Notificação | Se o aluno X não fez a atividade Y, notificar no dia seguinte | Média |
| Metas da turma | Meta **e forma de medição** definidas pela própria professora | Média |
| Grupos de acompanhamento | Ex.: "em alfabetização", "precisa de atenção"; criados pela professora | Média |
| Métricas personalizáveis | Métricas definidas pela professora, com histórico | Média/alta |

### Diretrizes de design

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

**Metas**
- A professora cria a meta **e escolhe como ela é medida**. O app não impõe critérios.
- A forma de medição precisa ser flexível (ex.: sim/não, percentual, número com valor-alvo, escala, texto livre).
- Avaliar compartilhar a estrutura de "tipo de medição" com as Métricas, já que o problema é parecido.

**Grupos e métricas**
- Grupos são **marcadores** criados pela professora (não listas fixas). Um aluno pode estar em vários grupos.
- Acompanhamento deve gerar **histórico com data**, para mostrar evolução.
- Métricas exigem estrutura flexível: tipo, escala, valores registrados ao longo do tempo.

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
- Funcionalidade de **exportar/importar** dados (JSON) e, possivelmente, relatório em PDF.

---

## 5. Modelo de dados (rascunho)

- `Turma` — nome, ano/série, período, ano letivo
- `Aluno` — nome, turma, dados mínimos necessários
- `Chamada` — turma + data (única por turma/dia)
- `RegistroPresenca` — chamada + aluno + presente/ausente + observação
- `Pendencia` — aluno + atividade + vínculo **opcional** com a ausência + status (pendente/entregue) + data prevista de lembrete
- `Meta` — turma + descrição + tipo de medição (definido pela professora) + valor-alvo opcional + status
- `RegistroMeta` — meta + valor + data (progresso ao longo do tempo)
- `Grupo` — turma + nome + descrição (ex.: "em alfabetização")
- `AlunoGrupo` — relação N:N entre aluno e grupo (com datas de entrada/saída para histórico)
- `Metrica` — turma + nome + tipo/escala
- `RegistroMetrica` — aluno + métrica + valor + data
- `Anotacao` — aluno + texto + data + tags

Todas as entidades seguem as regras da seção 4 (UUID, timestamps, soft delete).

---

## 6. Privacidade (LGPD)

- Dados de crianças: tratamento com cuidado especial (consentimento dos responsáveis, Art. 14).
- Coletar **apenas o mínimo necessário** de cada aluno.
- Manter tudo local no MVP; não enviar dados a serviços externos sem decisão explícita.

---

## 7. Contexto de mercado

Já existem vários "diários de classe digitais" (apps de secretarias estaduais, MSTECH, Tecsystem, Diário Fácil etc.), mas focados na parte **burocrática/oficial** (frequência, notas, conteúdos). ClassDojo e Google Classroom cobrem comunicação e entrega de atividades.

**Diferencial deste app:** substituir o **caderno pessoal da professora** — o acompanhamento pedagógico cotidiano que os sistemas oficiais não cobrem: faltas com observações, pendências por aluno, lembretes, anotações, grupos por necessidade, metas e registros de evolução, adequados a etapas em que não há avaliação por nota.

---

## 8. Decisões com a professora e próximos passos

**Respondido**
- *Por que anota no papel?* O sistema oficial serve só para a chamada. Todo o resto (faltas, anotações específicas, pendências) vai para um diário manual, que é o que o app deve substituir.
- *Como as metas são medidas?* A própria professora define as metas e a forma de medição.
- *Nome do app:* ainda não definido.

**Em aberto**
- Que métricas ela usaria no acompanhamento?
- Nome do app.

**Desenvolvimento**
1. Adicionar dependências: Room, WorkManager, Hilt, Navigation Compose.
2. Montar estrutura de pacotes MVVM (`data/` com entidades, DAOs e repositórios; `ui/` com telas e ViewModels; `di/`).
3. Implementar Turmas e Alunos (primeiro CRUD).
4. Registro de faltas → Pendências → Notificações.
5. Anotações, grupos, metas e métricas.
6. Backup/exportação.
