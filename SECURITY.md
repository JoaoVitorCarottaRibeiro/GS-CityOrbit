# Capítulo de Cybersecurity — CityOrbit
## FIAP Global Solution 2026 | 3ES — Engenharia de Software

---

## Objetivo

Este capítulo documenta a camada de segurança integrada à solução **CityOrbit**, uma plataforma de
Digital Twin urbano que consome dados climáticos da NASA POWER API e imagens de satélite ESRI para
simulação de cenários de impacto ambiental em cidades. A segurança é tratada como alicerce da
arquitetura, não como adendo.

---

## 1. Análise de Riscos e Ameaças (Threat Modeling)

### 1.1 Identificação de Ativos

A tabela abaixo classifica os ativos críticos da solução CityOrbit segundo o impacto em
Confidencialidade (C), Integridade (I) e Disponibilidade (D) — pilares da tríade CIA da ISO 27001.

| Ativo | Tipo | Classificação | C | I | D | Localização |
|---|---|---|---|---|---|---|
| **Chave secreta JWT (HMAC-SHA256)** | Criptográfico | Crítico | Alto | Alto | Alto | `application.properties` / env var `CITYORBIT_JWT_SECRET` |
| **Credenciais de usuários (admin/viewer)** | Identidade | Crítico | Alto | Alto | Médio | InMemoryUserDetailsManager + BCrypt(12) |
| **Banco de dados H2** | Dados | Alto | Alto | Alto | Alto | Em memória (dev) / Persistente (prod) |
| **Tokens JWT em circulação** | Sessão | Alto | Alto | Médio | Médio | Client-side + TokenBlacklist server-side |
| **Logs de auditoria** | Rastreabilidade | Alto | Médio | Alto | Alto | SLF4J → destino configurável |
| **NASA POWER API** | Integração externa | Médio | Médio | Alto | Alto | `https://power.larc.nasa.gov` (HTTPS) |
| **Imagens de satélite ESRI** | Dados geoespaciais | Médio | Baixo | Alto | Médio | CDN ArcGIS Online |
| **Dados de cidades e simulações** | Dados de negócio | Médio | Médio | Alto | Alto | Banco H2 / tabelas `TB_CITY`, `TB_SIMULATION` |
| **Código-fonte da aplicação** | Propriedade intelectual | Alto | Alto | Alto | Médio | Repositório Git privado |
| **Endpoints SOAP** | Integração | Médio | Médio | Alto | Médio | `/ws/**` — Spring-WS |

**Critério de classificação:**
- **Crítico**: comprometimento paralisa a operação ou vaza credenciais de acesso
- **Alto**: comprometimento causa dano operacional ou reputacional significativo
- **Médio**: comprometimento causa degradação de funcionalidade ou privacidade

---

### 1.2 Modelo de Ameaças (STRIDE)

Utilizamos o framework **STRIDE** (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of
Service, Elevation of Privilege) para mapear os vetores de ataque plausíveis sobre cada ativo crítico.

#### Diagrama de Fluxo de Dados (DFD — Nível 1)

```
[Browser / Cliente]
      │ HTTPS + JWT Bearer
      ▼
[RateLimitFilter] → bloqueia IPs com excesso de requisições
      │
[SecurityHeadersFilter] → injeta HSTS, CSP, X-Frame-Options
      │
[JwtAuthFilter] → valida assinatura HMAC-SHA256 + blacklist
      │
[Spring Security RBAC] → ADMIN / VIEWER / Anônimo
      │
      ├──► [REST Controllers] → CityController, SimulationController
      │           │
      │           ▼
      │    [Service Layer] ──► [H2 Database]
      │           │
      │           └──► [NASA POWER API] (HTTPS externo)
      │
      └──► [SOAP Endpoint] → SimulationEndpoint (/ws/**)
      
[AuditLogFilter] → registra TODAS as requisições (IP, user, método, status, tempo)
```

#### Tabela STRIDE

| # | Ameaça | Categoria STRIDE | Ativo-alvo | Vetor | Mitigação Implementada | Nível de Risco Residual |
|---|---|---|---|---|---|---|
| **T-01** | **Força bruta / Credential Stuffing** | Spoofing | Credenciais de usuário | POST `/api/auth/login` repetido | RateLimitFilter: 30 req/min (escrita) por IP; BCrypt(12) torna hashing lento | Baixo |
| **T-02** | **Interceptação em trânsito (MitM)** | Information Disclosure | Tokens JWT, dados de API | Rede não-confiável (Wi-Fi público, roteador comprometido) | HTTPS obrigatório em produção; HSTS com `max-age=31536000; includeSubDomains; preload`; JWT assinado (não apenas codificado) | Baixo |
| **T-03** | **Injeção de token JWT forjado** | Spoofing / Elevation of Privilege | Controle de acesso | Criação de JWT com `alg: none` ou chave fraca | HMAC-SHA256 com chave de 256 bits; validação de expiração; TokenBlacklist para tokens revogados | Baixo |
| **T-04** | **DDoS / Negação de Serviço** | Denial of Service | Disponibilidade da API | Flood de requisições HTTP | RateLimitFilter: 120 req/min (leitura), 30 req/min (escrita) por IP; retorna 429 Too Many Requests | Médio |
| **T-05** | **Clickjacking / XSS** | Tampering | Interface frontend | Iframe malicioso; injeção de script | `X-Frame-Options: SAMEORIGIN`; `X-Content-Type-Options: nosniff`; CSP: `default-src 'self'` | Baixo |
| **T-06** | **Escalonamento de privilégio** | Elevation of Privilege | Dados de cidades/simulações | Usuário VIEWER tentando executar DELETE | RBAC: DELETE exige `ROLE_ADMIN`; `@PreAuthorize` por método; retorna 403 | Baixo |
| **T-07** | **Acesso não autorizado ao banco de dados** | Information Disclosure | Banco H2 | Acesso direto ao H2 console | H2 console protegido por autenticação ADMIN; desabilitado em produção via env var `H2_CONSOLE_ENABLED=false` | Baixo |
| **T-08** | **Repúdio de ações** | Repudiation | Logs de auditoria | Usuário nega ter executado ação maliciosa | AuditLogFilter registra IP, usuário autenticado, método, path e status em TODAS as requisições | Baixo |
| **T-09** | **Injeção de SQL** | Tampering | Banco de dados | Parâmetros maliciosos em queries | Spring Data JPA com queries parametrizadas; sem SQL concatenado; Bean Validation nas entradas | Baixo |
| **T-10** | **Comprometimento da integração NASA** | Tampering / Spoofing | Dados climáticos | DNS poisoning; resposta forjada da API | HTTPS com validação de certificado; RestTemplate configurado sem `trustAll`; dados validados antes de persistir | Médio |

---

## 2. Arquitetura de Segurança (Controles)

### 2.1 Controles de Acesso

**Princípio aplicado:** Privilégio Mínimo (Least Privilege) + Autenticação de Múltiplos Fatores (MFA).

#### 2.1.1 Autenticação em Dois Fatores (MFA — TOTP)

O CityOrbit implementa **TOTP (Time-based One-Time Password)** compatível com Google Authenticator e
Authy, seguindo o padrão **RFC 6238**.

**Fluxo de autenticação com MFA ativo:**

```
Passo 1: POST /api/auth/login
         Corpo: { "username": "admin", "password": "***" }
         → Credenciais válidas + MFA ativo
         ← { "mfaRequired": true, "sessionId": "uuid-temp" }  [sessão válida por 5 min]

Passo 2: POST /api/auth/mfa/complete
         Corpo: { "sessionId": "uuid-temp", "code": "123456" }
         → TOTP válido (janela ±30s)
         ← { "token": "eyJ...", "username": "admin", "role": "ADMIN" }
```

**Fluxo de configuração do MFA:**

```
POST /api/auth/mfa/setup    → retorna { "secret": "BASE32SECRET", "qrImageUrl": "..." }
POST /api/auth/mfa/activate → { "code": "123456" } confirma primeiro código → ativa MFA
```

**Implementação:** `MfaService.java` usa `GoogleAuthenticator` (googleauth 1.5.0) com:
- Segredo de 80 bits gerado por `SecureRandom`
- Código de 6 dígitos, janela de 30 segundos
- Tolerância de ±1 janela (30s) para drift de relógio

#### 2.1.2 Controle de Acesso Baseado em Papéis (RBAC)

| Operação | Anônimo | VIEWER | ADMIN |
|---|---|---|---|
| `GET /api/**` (leitura) | ✓ | ✓ | ✓ |
| `POST /api/**` (criação) | ✗ | ✓ | ✓ |
| `PUT /api/**` (atualização) | ✗ | ✓ | ✓ |
| `DELETE /api/**` (exclusão) | ✗ | ✗ | ✓ |
| `POST /api/security/incident/**` | ✗ | ✗ | ✓ |
| `DELETE /api/auth/mfa/disable` | ✗ | ✗ | ✓ |
| `GET /h2-console/**` | ✗ | ✗ | ✓ |

#### 2.1.3 Gestão de Sessão

- **Stateless JWT:** nenhuma sessão armazenada no servidor (Zero Trust)
- **Expiração:** 8 horas (configurável via `CITYORBIT_JWT_EXPIRATION`)
- **Revogação imediata:** `TokenBlacklist` invalida tokens individuais (logout) ou todos (lockdown de incidente)
- **Rotação de chave:** em caso de comprometimento, basta alterar `CITYORBIT_JWT_SECRET` para invalidar todos os tokens em circulação

---

### 2.2 Proteção de Dados

#### Em Trânsito (Data in Transit)

| Mecanismo | Onde aplicado | Detalhes |
|---|---|---|
| **TLS 1.2+ (HTTPS)** | Toda comunicação | Obrigatório em produção via configuração do servidor (Nginx, AWS ALB) |
| **HSTS** | Cabeçalho HTTP | `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload` |
| **JWT assinado** | API REST | HMAC-SHA256 — o conteúdo do token não pode ser forjado sem a chave secreta |
| **CORS restritivo** | Cabeçalhos HTTP | Origens permitidas configuradas via `CITYORBIT_CORS_ORIGINS` (não `*`) |

#### Em Repouso (Data at Rest)

| Mecanismo | Onde aplicado | Detalhes |
|---|---|---|
| **BCrypt (fator 12)** | Senhas de usuários | ~300ms por hash — resistente a ataques de dicionário e GPU |
| **Senha do banco H2** | Banco de dados | `CITYORBIT_DB_PASS` — sem acesso anônimo ao banco |
| **Variáveis de ambiente** | Credenciais e segredos | Nenhum segredo em código-fonte ou application.properties commitado |
| **Criptografia em produção** | Banco persistente (prod) | PostgreSQL com `pgcrypto` ou AWS RDS com encryption at rest (AES-256) |

#### Anonimização e Minimização

- O sistema não coleta dados pessoais além de `username` (pseudônimo)
- Coordenadas geográficas são de cidades (não de indivíduos)
- Logs de auditoria usam o username autenticado — em produção, aplicar pseudonimização após 90 dias

---

### 2.3 Segurança da Infraestrutura

#### Arquitetura Zero Trust

O CityOrbit implementa os princípios de **Zero Trust Architecture (NIST SP 800-207)**:

1. **"Never trust, always verify":** cada requisição valida o JWT independentemente, sem confiar em
   sessões ou localização de rede
2. **Sessão stateless:** o servidor não mantém estado de autenticação — sem cookies de sessão
3. **Acesso mínimo:** cada role tem exatamente as permissões necessárias para sua função
4. **Monitoramento contínuo:** AuditLogFilter registra 100% das requisições API

#### Filtros de Segurança (cadeia de execução)

```
RateLimitFilter        → 1º: bloqueia IPs abusivos (429) antes de qualquer processamento
SecurityHeadersFilter  → 2º: injeta cabeçalhos de segurança em todas as respostas
JwtAuthFilter          → 3º: valida e extrai identidade do token Bearer
AuditLogFilter         → 4º (pós-auth): registra identidade + resultado da requisição
```

#### Cabeçalhos de Segurança HTTP (OWASP Secure Headers)

| Cabeçalho | Valor | Proteção |
|---|---|---|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` | Força HTTPS; previne downgrade |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self' cdn.jsdelivr.net unpkg.com` | Previne XSS |
| `X-Frame-Options` | `SAMEORIGIN` | Previne clickjacking |
| `X-Content-Type-Options` | `nosniff` | Previne MIME-sniffing |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Controla vazamento de URL |
| `Permissions-Policy` | `geolocation=(), microphone=(), camera=()` | Desabilita APIs sensíveis de browser |

#### Rate Limiting

| Tipo de operação | Limite | Resposta ao exceder |
|---|---|---|
| Leitura (GET) | 120 req/min por IP | 429 Too Many Requests |
| Escrita (POST/PUT/DELETE/PATCH) | 30 req/min por IP | 429 Too Many Requests |

**Nota de produção:** em alta escala, usar Redis + Nginx `limit_req_zone` ou AWS WAF para distribuição.

#### Monitoramento e Rastreabilidade

O `AuditLogFilter` gera entradas estruturadas para cada requisição:

```
[AUDIT] 2026-06-03T14:32:11Z | ip=192.168.1.100 | user=admin | POST /api/cities | status=201 | 45ms
[AUDIT-ALERT] POST /api/auth/login → 401 | ip=10.0.0.5 | user=anonymous
```

**Em produção:** encaminhar logs para sistema imutável (AWS CloudWatch, Elastic Stack / ELK) com
retenção mínima de **90 dias** (LGPD Art. 46) e **1 ano** para logs de segurança (ISO 27001 A.12.4).

---

## 3. Governança e Compliance

### 3.1 Alinhamento Normativo — ISO/IEC 27001:2022

A tabela abaixo mapeia os controles ISO 27001 implementados no CityOrbit, com evidência técnica de
cada um.

| Controle ISO 27001 | Descrição | Implementação no CityOrbit | Arquivo de evidência |
|---|---|---|---|
| **A.5.15** | Controle de acesso | RBAC com ADMIN e VIEWER; `@PreAuthorize` por endpoint | `SecurityConfig.java` |
| **A.5.16** | Gerenciamento de identidade | InMemoryUserDetailsManager → prod: banco de dados + MFA | `SecurityConfig.java` |
| **A.5.17** | Informações de autenticação | BCrypt(12) para senhas; TOTP para MFA; JWT para sessão | `JwtUtil.java`, `MfaService.java` |
| **A.8.5** | Autenticação segura | Login em 2 fatores (senha + TOTP); expiração de sessão 8h | `AuthController.java`, `MfaController.java` |
| **A.8.8** | Gerenciamento de vulnerabilidades | HSTS, CSP, X-Frame-Options; rate limiting; validação de entrada | `SecurityHeadersFilter.java` |
| **A.8.15** | Log de eventos | 100% das requisições API auditadas com IP, usuário e status | `AuditLogFilter.java` |
| **A.8.16** | Monitoramento de atividades | Alertas automáticos em respostas 4xx/5xx no audit log | `AuditLogFilter.java` |
| **A.8.20** | Segurança de rede | CORS restritivo; Rate Limiting por IP; HTTPS obrigatório | `SecurityConfig.java`, `RateLimitFilter.java` |
| **A.8.24** | Uso de criptografia | HMAC-SHA256 (JWT), BCrypt(12) (senhas), TLS 1.2+ (trânsito) | `JwtUtil.java`, `SecurityConfig.java` |
| **A.5.26** | Resposta a incidentes de segurança | Plano completo em 3 fases implementado como API funcional | `SecurityIncidentController.java` |
| **A.5.29** | Continuidade de segurança | Lockdown global de tokens; restauração de estado no reinício | `TokenBlacklist.java` |
| **A.5.34** | Privacidade e proteção de dados | Endpoints LGPD; minimização de dados; base legal documentada | `LgpdController.java` |

**Gestão de Riscos (ISO 27001 Cláusula 6.1):**

O processo de gestão de riscos adotado segue o ciclo:

```
Identificação → Análise (Tabela STRIDE) → Avaliação (nível de risco) → Tratamento (controles) → Monitoramento (audit log)
```

Riscos residuais considerados aceitáveis para o contexto acadêmico:
- Banco H2 em memória (prod: substituir por PostgreSQL com encryption at rest)
- Armazenamento de segredos MFA em memória (prod: substituir por KMS / HashiCorp Vault)

---

### 3.2 Privacidade — LGPD (Lei 13.709/2018)

#### Base Legal para Tratamento de Dados

| Dado Tratado | Base Legal (LGPD Art. 7º) | Finalidade |
|---|---|---|
| `username` + senha (hash) | Legítimo interesse / execução de contrato | Autenticação e autorização |
| Logs de auditoria (IP, username, timestamp) | Cumprimento de obrigação legal | Rastreabilidade e segurança |
| Dados de cidades (coordenadas geográficas) | Legítimo interesse | Simulação de impacto climático |
| Dados de simulações | Legítimo interesse | Análise e planejamento urbano |

**Importante:** coordenadas são de **cidades** (entidades geográficas), não de pessoas físicas.
O sistema não coleta localização individual de usuários.

#### Direitos dos Titulares (LGPD Cap. III)

O endpoint `/api/lgpd/` implementa os direitos garantidos pela LGPD:

| Artigo LGPD | Direito | Endpoint | Método |
|---|---|---|---|
| Art. 18, I | Confirmação de existência de dados | `GET /api/lgpd/my-data` | Autenticado |
| Art. 18, II | Acesso aos dados | `GET /api/lgpd/my-data` | Autenticado |
| Art. 18, VI | Exclusão dos dados | `DELETE /api/lgpd/my-data` | Autenticado |
| Art. 18, VII | Informação sobre compartilhamento | `GET /api/lgpd/privacy-notice` | Público |
| Art. 8, §5 | Registro de consentimento | `POST /api/lgpd/consent` | Autenticado |

#### Comunicação de Incidentes (LGPD Art. 48)

Em caso de incidente de segurança com potencial impacto a titulares de dados, o sistema prevê:

- **Prazo:** notificação à ANPD e titulares afetados em até **72 horas**
- **Mecanismo:** relatório pós-incidente gerado automaticamente pela Fase 3 do plano de resposta
- **Conteúdo:** data/hora do incidente, natureza dos dados afetados, medidas de contenção adotadas

#### Minimização e Retenção

- **Minimização (Art. 6, III):** o sistema coleta apenas dados estritamente necessários
- **Retenção de logs:** 90 dias para logs operacionais; 1 ano para logs de segurança
- **Anonimização:** logs antigos devem ter o campo `user` substituído por hash SHA-256 após o período de retenção

---

## 4. Plano de Resiliência e Continuidade

### 4.1 Plano de Resposta a Incidentes de Segurança

**Base normativa:** ISO 27001 A.5.26 | NIST SP 800-61 Rev.2 | LGPD Art. 48

#### Gatilhos de Acionamento

O plano deve ser acionado quando qualquer um dos seguintes eventos for detectado:

- Falhas de autenticação repetidas (>50 em 5 min de um mesmo IP)
- Token JWT com assinatura inválida detectado mais de 10 vezes
- Acesso ao banco de dados fora do horário operacional
- Resposta anômala da NASA POWER API (dados inconsistentes)
- Alerta de intrusion detection (IDS externo)

---

#### FASE 1 — CONTENÇÃO (Containment)

**Objetivo:** Impedir imediatamente que o ataque cause mais danos.

**Tempo alvo:** < 1 minuto após confirmação do incidente.

| Passo | Ação | Executor | Endpoint/Ferramenta |
|---|---|---|---|
| 1 | Confirmar o incidente (verdadeiro positivo vs. falso positivo) | Administrador | Revisar AuditLog |
| 2 | **Revogar TODOS os tokens JWT** — desconectar todos os usuários | Administrador | `POST /api/security/incident/contain` |
| 3 | Registrar IP(s) suspeito(s) identificados nos logs | Administrador | `GET /api/security/incident/audit-events` |
| 4 | Bloquear IP(s) no firewall/WAF | Infra | Firewall externo / AWS WAF |
| 5 | Notificar equipe de segurança (CERT interno) | Administrador | E-mail / canal de alertas |

**Resultado esperado:** sistema em modo de contenção — nenhum usuário autenticado, acesso bloqueado.

**Implementação:** `TokenBlacklist.revokeAll()` ativa o flag `lockdownMode`, rejeitando qualquer
token válido no `JwtAuthFilter`. O estado é persistido no banco de dados via `IncidentEventRepository`.

---

#### FASE 2 — ERRADICAÇÃO (Eradication)

**Objetivo:** Remover a causa raiz do incidente.

**Tempo alvo:** 30 minutos a 4 horas (dependendo da complexidade).

| Passo | Ação | Executor | Detalhes |
|---|---|---|---|
| 1 | Analisar logs de auditoria completos | Analista de Segurança | `GET /api/security/incident/audit-events` |
| 2 | Identificar o vetor de ataque (credencial comprometida? JWT forjado? DDoS?) | Analista | Correlação de logs |
| 3 | **Rotacionar segredos:** chave JWT, senhas de usuários | Administrador | Alterar `CITYORBIT_JWT_SECRET` e `CITYORBIT_ADMIN_PASS` |
| 4 | **Desativar MFA comprometido** e reemitir segredos TOTP | Administrador | `DELETE /api/auth/mfa/disable` |
| 5 | Aplicar patch na vulnerabilidade explorada | Dev | Correção de código + novo deploy |
| 6 | Registrar ações realizadas | Administrador | `POST /api/security/incident/eradicate` |

---

#### FASE 3 — RECUPERAÇÃO (Recovery)

**Objetivo:** Restaurar operação normal com segurança aumentada.

**Tempo alvo:** 2 a 24 horas após erradicação confirmada.

| Passo | Ação | Executor | Detalhes |
|---|---|---|---|
| 1 | Verificar integridade dos dados (simulações e cidades) | DBA / Analista | Comparar com último backup |
| 2 | **Encerrar lockdown** — permitir novos logins com novas credenciais | Administrador | `POST /api/security/incident/recover` |
| 3 | Ativar monitoramento reforçado por **72 horas** | Infra | Alertas com limiar reduzido |
| 4 | **Comunicar titulares afetados** (se dados pessoais foram expostos) | DPO | Prazo LGPD Art.48: 72h |
| 5 | Gerar **Post-Incident Report (PIR)** | Equipe | `GET /api/security/incident/status` |
| 6 | Revisar e atualizar políticas de segurança | CISO | Incorporar lições aprendidas |

**Métricas de Recuperação:**

| Métrica | Meta |
|---|---|
| **RTO** (Recovery Time Objective) | < 4 horas |
| **RPO** (Recovery Point Objective) | < 1 hora (último backup) |
| **MTTD** (Mean Time to Detect) | < 5 minutos (via alerts do AuditLogFilter) |
| **MTTC** (Mean Time to Contain) | < 1 minuto (lockdown automático) |

---

#### Fluxo de Decisão do Incidente

```
Alerta detectado
      │
      ▼
 Falso positivo? ──Sim──► Documentar e fechar
      │Não
      ▼
POST /api/security/incident/contain
  (LOCKDOWN — Fase 1)
      │
      ▼
Analisar logs + identificar causa raiz
      │
      ▼
POST /api/security/incident/eradicate
  (Registrar erradicação — Fase 2)
      │
      ▼
Confirmar erradicação completa?
      │Sim
      ▼
POST /api/security/incident/recover
  (Restaurar — Fase 3)
      │
      ▼
Comunicar ANPD se dados pessoais afetados (72h — LGPD Art.48)
      │
      ▼
Post-Incident Report + atualização de políticas
```

---

## 5. Conclusão

A solução CityOrbit implementa segurança em **profundidade** (Defense in Depth), cobrindo todas as
camadas:

- **Prevenção:** MFA, RBAC, HTTPS/HSTS, rate limiting, validação de entrada, CSP
- **Detecção:** audit log 100% das requisições, alertas automáticos em erros 4xx
- **Resposta:** plano de incidentes em 3 fases totalmente automatizável via API
- **Conformidade:** ISO 27001 A.5–A.8, LGPD Art. 18 e Art. 48, NIST SP 800-61

A arquitetura Zero Trust (stateless JWT + verificação por requisição) garante que o comprometimento
de uma sessão não comprometa outras, e o mecanismo de lockdown global permite contenção em menos de
1 minuto.

---

*Documento elaborado para a disciplina de Cybersecurity — FIAP Global Solution 1º Semestre 2026.*
*Normas de referência: ISO/IEC 27001:2022 | NIST SP 800-61 Rev.2 | NIST SP 800-207 | LGPD (Lei 13.709/2018)*
