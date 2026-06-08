const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, VerticalAlign, PageNumber, PageBreak, LevelFormat
} = require("docx");
const fs = require("fs");

const OUTPUT = "C:\\Users\\aleri\\Downloads\\CityOrbit-Cybersecurity-SemCabecalho.docx";

// ── Cores ──────────────────────────────────────────────────────────
const NAVY   = "0A1628";
const CYAN   = "0099BB";
const GREEN  = "1A7A4A";
const RED    = "C0392B";
const AMBER  = "D4700A";
const WHITE  = "FFFFFF";
const GRAY   = "F4F6F9";
const DGRAY  = "DDDDDD";
const BLACK  = "1A1F2E";

// ── Bordas ─────────────────────────────────────────────────────────
const brd  = (c = DGRAY, s = 4) => ({ style: BorderStyle.SINGLE, size: s, color: c });
const brds = (c = DGRAY) => ({ top: brd(c), bottom: brd(c), left: brd(c), right: brd(c) });

// ── Helpers ────────────────────────────────────────────────────────
function run(text, size = 22, color = BLACK, opts = {}) {
  return new TextRun({ text, size, color, font: "Arial", ...opts });
}
function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    children: [new TextRun({ text, bold: true, size: 36, color: NAVY, font: "Arial" })],
    spacing: { before: 400, after: 200 },
    border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: CYAN } }
  });
}
function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    children: [new TextRun({ text, bold: true, size: 28, color: CYAN, font: "Arial" })],
    spacing: { before: 280, after: 140 }
  });
}
function h3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    children: [new TextRun({ text, bold: true, size: 24, color: BLACK, font: "Arial" })],
    spacing: { before: 200, after: 100 }
  });
}
function bodyPara(text) {
  return new Paragraph({
    children: [run(text)],
    spacing: { before: 100, after: 100 },
    alignment: AlignmentType.JUSTIFIED
  });
}
function bullet(text) {
  return new Paragraph({
    numbering: { reference: "bullets", level: 0 },
    children: [run(text)],
    spacing: { before: 60, after: 60 }
  });
}
function numbered(n, text) {
  return new Paragraph({
    children: [run(`${n}. ${text}`)],
    spacing: { before: 60, after: 60 },
    indent: { left: 400 }
  });
}
function codeBlock(lines) {
  return lines.map(line => new Paragraph({
    children: [new TextRun({ text: line, font: "Courier New", size: 18, color: "1A1A2E" })],
    spacing: { before: 20, after: 20 },
    shading: { fill: "EEF1F5", type: ShadingType.CLEAR },
    indent: { left: 400 }
  }));
}
function spacer(n = 1) {
  return Array.from({ length: n }, () =>
    new Paragraph({ children: [], spacing: { before: 60, after: 60 } })
  );
}
function pageBreak() {
  return new Paragraph({ children: [new PageBreak()] });
}
function noteBox(text, color = CYAN) {
  return new Paragraph({
    children: [run(text, 20, BLACK, { italics: true })],
    spacing: { before: 120, after: 120 },
    indent: { left: 400, right: 200 },
    border: { left: { style: BorderStyle.SINGLE, size: 16, color: color } },
    shading: { fill: GRAY, type: ShadingType.CLEAR }
  });
}
function makeTable(headers, rows, colWidths, headerBg = NAVY) {
  const totalW = colWidths.reduce((a, b) => a + b, 0);
  const headerRow = new TableRow({
    tableHeader: true,
    children: headers.map((h, i) => new TableCell({
      borders: brds(headerBg),
      width: { size: colWidths[i], type: WidthType.DXA },
      shading: { fill: headerBg, type: ShadingType.CLEAR },
      margins: { top: 100, bottom: 100, left: 160, right: 160 },
      verticalAlign: VerticalAlign.CENTER,
      children: [new Paragraph({
        children: [new TextRun({ text: h, bold: true, size: 20, color: WHITE, font: "Arial" })],
        alignment: AlignmentType.CENTER
      })]
    }))
  });
  const dataRows = rows.map((row, ri) => new TableRow({
    children: row.map((cell, ci) => new TableCell({
      borders: brds(DGRAY),
      width: { size: colWidths[ci], type: WidthType.DXA },
      shading: { fill: ri % 2 === 0 ? WHITE : GRAY, type: ShadingType.CLEAR },
      margins: { top: 80, bottom: 80, left: 160, right: 160 },
      verticalAlign: VerticalAlign.TOP,
      children: [new Paragraph({ children: [run(cell, 20)], alignment: AlignmentType.LEFT })]
    }))
  }));
  return new Table({ width: { size: totalW, type: WidthType.DXA }, columnWidths: colWidths, rows: [headerRow, ...dataRows] });
}

// ══════════════════════════════════════════════════════════════════
// CONTEÚDO
// ══════════════════════════════════════════════════════════════════
const children = [];

// ── CAPA ──────────────────────────────────────────────────────────
children.push(...spacer(4));
children.push(new Paragraph({
  children: [new TextRun({ text: "CITYORBIT", bold: true, size: 96, color: NAVY, font: "Arial" })],
  alignment: AlignmentType.CENTER, spacing: { before: 0, after: 160 }
}));
children.push(new Paragraph({
  children: [new TextRun({ text: "Digital Twin Urbano", size: 44, color: CYAN, font: "Arial" })],
  alignment: AlignmentType.CENTER, spacing: { before: 0, after: 400 }
}));
children.push(new Paragraph({
  children: [new TextRun({ text: "Capitulo de Cybersecurity  |  Global Solution 2026", bold: true, size: 30, color: WHITE, font: "Arial" })],
  alignment: AlignmentType.CENTER, spacing: { before: 0, after: 400 },
  shading: { fill: NAVY, type: ShadingType.CLEAR },
  border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: CYAN } }
}));
children.push(...spacer(2));
children.push(new Paragraph({
  children: [new TextRun({ text: "Integrantes do Grupo", bold: true, size: 26, color: NAVY, font: "Arial" })],
  alignment: AlignmentType.CENTER, spacing: { before: 200, after: 160 }
}));
for (const m of [
  "Arthur Bueno de Oliveira  |  RM 558396",
  "Joao Vitor Carotta Ribeiro  |  RM 555187",
  "Victor Magdaleno Marcos  |  RM 556729"
]) {
  children.push(new Paragraph({
    children: [new TextRun({ text: m, size: 24, color: BLACK, font: "Arial" })],
    alignment: AlignmentType.CENTER, spacing: { before: 80, after: 80 }
  }));
}
children.push(...spacer(2));
for (const info of [
  "Engenharia de Software  |  3ES",
  "Cybersecurity  |  Global Solution 2026",
  "FIAP  |  1 Semestre 2026"
]) {
  children.push(new Paragraph({
    children: [new TextRun({ text: info, size: 22, color: BLACK, font: "Arial", italics: true })],
    alignment: AlignmentType.CENTER, spacing: { before: 80, after: 80 }
  }));
}
children.push(pageBreak());

// ── SUMÁRIO ────────────────────────────────────────────────────────
children.push(h1("Sumario"));
const toc = [
  "1.  Identificacao de Ativos",
  "2.  Modelo de Ameacas (Threat Modeling — STRIDE)",
  "3.  Controles de Acesso",
  "4.  Protecao de Dados",
  "5.  Seguranca da Infraestrutura",
  "6.  Alinhamento Normativo — ISO/IEC 27001:2022",
  "7.  Privacidade e LGPD",
  "8.  Plano de Resposta a Incidentes",
];
toc.forEach(t => children.push(new Paragraph({
  children: [run(t, 22, BLACK)], spacing: { before: 100, after: 100 }
})));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 1. IDENTIFICAÇÃO DE ATIVOS
// ═══════════════════════════════════════════════════════════════════
children.push(h1("1. Identificacao de Ativos"));
children.push(bodyPara(
  "A identificacao de ativos e o primeiro passo de qualquer programa de seguranca da informacao (ISO 27001 Clausula 6.1). O CityOrbit gerencia os seguintes ativos criticos, classificados pela tríade CIA (Confidencialidade, Integridade, Disponibilidade)."
));
children.push(...spacer(1));
children.push(makeTable(
  ["Ativo", "Tipo", "Classificacao", "C", "I", "D", "Localizacao"],
  [
    ["Chave secreta JWT (HMAC-SHA256)", "Criptografico", "CRITICO", "Alto", "Alto", "Alto", "Variavel de ambiente CITYORBIT_JWT_SECRET"],
    ["Credenciais de usuarios (admin/viewer)", "Identidade", "CRITICO", "Alto", "Alto", "Medio", "InMemoryUserDetailsManager + BCrypt(12)"],
    ["Segredos TOTP de MFA", "Criptografico", "CRITICO", "Alto", "Alto", "Medio", "Banco H2 — tabela TB_MFA_SECRET"],
    ["Banco de dados H2", "Dados", "ALTO", "Alto", "Alto", "Alto", "Em memoria (dev) / Persistente (prod)"],
    ["Tokens JWT em circulacao", "Sessao", "ALTO", "Alto", "Medio", "Medio", "Client-side + TokenBlacklist server-side"],
    ["Logs de auditoria", "Rastreabilidade", "ALTO", "Medio", "Alto", "Alto", "SLF4J — destino configuravel (CloudWatch/ELK)"],
    ["Registros LGPD (consentimento)", "Compliance", "ALTO", "Alto", "Alto", "Medio", "Banco H2 — tabela TB_LGPD_CONSENT"],
    ["NASA POWER API", "Integracao externa", "MEDIO", "Medio", "Alto", "Alto", "https://power.larc.nasa.gov (HTTPS)"],
    ["Dados de cidades e simulacoes", "Dados de negocio", "MEDIO", "Medio", "Alto", "Alto", "Banco H2 — TB_CITY, TB_SIMULATION"],
    ["Codigo-fonte da aplicacao", "Propriedade intelectual", "ALTO", "Alto", "Alto", "Medio", "Repositorio Git privado"],
  ],
  [3200, 2000, 1600, 700, 700, 700, 3100]
));
children.push(...spacer(1));
children.push(noteBox(
  "Criterio de classificacao — CRITICO: comprometimento paralisa a operacao ou vaza credenciais de acesso. ALTO: dano operacional ou reputacional significativo. MEDIO: degradacao de funcionalidade ou privacidade.",
  CYAN
));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 2. MODELO DE AMEAÇAS
// ═══════════════════════════════════════════════════════════════════
children.push(h1("2. Modelo de Ameacas — STRIDE"));
children.push(bodyPara(
  "O modelo de ameacas foi elaborado utilizando o framework STRIDE (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege), metodologia recomendada pelo NIST e amplamente adotada pela industria para analise sistematica de riscos em aplicacoes."
));
children.push(...spacer(1));
children.push(h2("2.1 Diagrama de Fluxo de Dados (DFD — Nivel 1)"));
children.push(...codeBlock([
  "  [Browser / Cliente]",
  "        |",
  "        v  HTTPS + JWT Bearer",
  "  [RateLimitFilter] ——> bloqueia IPs abusivos (429)",
  "        |",
  "  [SecurityHeadersFilter] ——> HSTS, CSP, X-Frame-Options",
  "        |",
  "  [JwtAuthFilter] ——> valida HMAC-SHA256 + blacklist",
  "        |",
  "  [Spring Security RBAC] ——> ADMIN / VIEWER / Anonimo",
  "        |",
  "        +——————> [REST Controllers] ——> [H2 Database]",
  "        |               |",
  "        |               +——> [NASA POWER API] (HTTPS externo)",
  "        |",
  "        +——————> [SOAP Endpoint] ——> [SimulationService]",
  "",
  "  [AuditLogFilter] ——> registra TODAS as requisicoes",
]));
children.push(...spacer(1));
children.push(h2("2.2 Tabela de Ameacas STRIDE"));
children.push(makeTable(
  ["#", "Ameaca", "Categoria", "Ativo-alvo", "Vetor de Ataque", "Mitigacao Implementada", "Risco Residual"],
  [
    ["T-01", "Forca bruta / Credential Stuffing", "Spoofing", "Credenciais de usuario", "POST /api/auth/login repetido", "RateLimitFilter: 30 req/min por IP + BCrypt(12) ~300ms por hash", "BAIXO"],
    ["T-02", "Interceptacao em transito (MitM)", "Info. Disclosure", "Tokens JWT, dados de API", "Rede nao confiavel", "HTTPS obrigatorio + HSTS max-age=31536000 + JWT assinado HMAC-SHA256", "BAIXO"],
    ["T-03", "JWT forjado (alg:none)", "Spoofing / Elevacao", "Controle de acesso", "Criacao de JWT com algoritmo none ou chave fraca", "HMAC-SHA256 obrigatorio + validacao de expiracao + TokenBlacklist", "BAIXO"],
    ["T-04", "DDoS / Negacao de Servico", "Denial of Service", "Disponibilidade da API", "Flood de requisicoes HTTP", "RateLimitFilter: 120 req/min (leitura), 30/min (escrita) por IP — HTTP 429", "MEDIO"],
    ["T-05", "Clickjacking / XSS", "Tampering", "Interface frontend", "Iframe malicioso ou injecao de script", "X-Frame-Options: SAMEORIGIN + CSP default-src 'self' + X-Content-Type-Options: nosniff", "BAIXO"],
    ["T-06", "Escalonamento de privilegio", "Elevation of Privilege", "Dados e operacoes criticas", "Usuario VIEWER tentando DELETE", "RBAC: DELETE exige ROLE_ADMIN + @PreAuthorize por metodo — HTTP 403", "BAIXO"],
    ["T-07", "Acesso nao autorizado ao banco", "Info. Disclosure", "Banco H2", "Acesso direto ao console H2", "H2 console exige autenticacao ADMIN + desabilitado em prod (H2_CONSOLE_ENABLED=false)", "BAIXO"],
    ["T-08", "Repudio de acoes maliciosas", "Repudiation", "Logs de auditoria", "Usuario nega ter executado acao", "AuditLogFilter registra IP, usuario autenticado, metodo, path e status em TODAS as requests", "BAIXO"],
    ["T-09", "Injecao de SQL", "Tampering", "Banco de dados", "Parametros maliciosos em queries", "Spring Data JPA com queries parametrizadas — sem SQL concatenado; Bean Validation nas entradas", "BAIXO"],
    ["T-10", "Bypass do 2o fator (MFA)", "Spoofing", "Autenticacao", "Reutilizacao de sessao ou brute-force TOTP", "Sessao pendente com TTL de 5 minutos + TOTP com janela de 30s + invalidacao imediata apos uso", "BAIXO"],
  ],
  [500, 2200, 1500, 1800, 2200, 2800, 1000]
));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 3. CONTROLES DE ACESSO
// ═══════════════════════════════════════════════════════════════════
children.push(h1("3. Controles de Acesso"));
children.push(bodyPara(
  "O CityOrbit implementa controle de acesso em multiplas camadas, combinando autenticacao de dois fatores (MFA), controle baseado em papeis (RBAC) e gestao de sessao stateless via JWT."
));

children.push(h2("3.1 Autenticacao Multi-Fator (MFA) — TOTP RFC 6238"));
children.push(bodyPara(
  "Implementado via biblioteca GoogleAuth 1.5.0, o MFA segue o padrao TOTP (Time-based One-Time Password) da RFC 6238, compativel com Google Authenticator e Authy. Os segredos TOTP sao persistidos no banco de dados (TB_MFA_SECRET) para sobreviver a reinicializacoes do servidor."
));
children.push(...spacer(1));
children.push(h3("Fluxo de Login com MFA Ativo:"));
children.push(...codeBlock([
  "Passo 1: POST /api/auth/login",
  "  Corpo: { \"username\": \"admin\", \"password\": \"***\" }",
  "  Credenciais validas + MFA ativo ->",
  "  Retorno: { \"mfaRequired\": true, \"sessionId\": \"uuid\" }",
  "           (sessao valida por 5 minutos)",
  "",
  "Passo 2: POST /api/auth/mfa/complete",
  "  Corpo: { \"sessionId\": \"uuid\", \"code\": \"123456\" }",
  "  TOTP valido (janela +-30s) ->",
  "  Retorno: { \"token\": \"eyJ...\", \"username\": \"admin\", \"role\": \"ADMIN\" }",
]));
children.push(...spacer(1));
children.push(h3("Configuracao do MFA:"));
children.push(...codeBlock([
  "POST /api/auth/mfa/setup    -> retorna { secret, otpAuthUri, qrImageUrl }",
  "POST /api/auth/mfa/activate -> { \"code\": \"123456\" } confirma + persiste no banco",
  "DELETE /api/auth/mfa/disable -> desativa (somente ADMIN)",
]));

children.push(h2("3.2 Controle Baseado em Papeis (RBAC)"));
children.push(makeTable(
  ["Operacao", "Anonimo", "VIEWER", "ADMIN"],
  [
    ["GET /api/** (leitura)",              "Sim", "Sim", "Sim"],
    ["POST /api/** (criacao)",             "Nao", "Sim", "Sim"],
    ["PUT /api/** (atualizacao)",          "Nao", "Sim", "Sim"],
    ["DELETE /api/** (exclusao)",          "Nao", "Nao", "Sim"],
    ["POST /api/security/incident/**",     "Nao", "Nao", "Sim"],
    ["DELETE /api/auth/mfa/disable",       "Nao", "Nao", "Sim"],
    ["GET /h2-console/**",                 "Nao", "Nao", "Sim"],
    ["GET /api/lgpd/** (dados pessoais)",  "Nao", "Sim", "Sim"],
  ],
  [4800, 1800, 1800, 1600]
));

children.push(h2("3.3 Gestao de Sessao — Zero Trust"));
for (const b of [
  "Sessao STATELESS com JWT: nenhuma sessao armazenada no servidor (NIST SP 800-207)",
  "Expiracao: 8 horas (configuravel via CITYORBIT_JWT_EXPIRATION)",
  "Revogacao imediata: TokenBlacklist invalida tokens individuais (logout) ou todos (lockdown)",
  "BCrypt fator 12 para senhas: ~300ms por hash, resistente a GPU cracking",
  "Aviso de MFA obrigatorio: login de ADMIN sem MFA retorna mfaWarning com instrucoes",
]) children.push(bullet(b));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 4. PROTEÇÃO DE DADOS
// ═══════════════════════════════════════════════════════════════════
children.push(h1("4. Protecao de Dados"));
children.push(bodyPara(
  "A protecao de dados e implementada em duas dimensoes: dados em transito (comunicacao de rede) e dados em repouso (armazenamento). O CityOrbit aplica criptografia em todas as camadas conforme ISO 27001 A.8.24."
));

children.push(h2("4.1 Dados em Transito (Data in Transit)"));
children.push(makeTable(
  ["Mecanismo", "Onde Aplicado", "Detalhes Tecnicos"],
  [
    ["TLS 1.2+ (HTTPS)", "Toda comunicacao", "Obrigatorio em producao via Nginx/AWS ALB. Certificado TLS valido exigido."],
    ["HSTS", "Cabecalho HTTP", "Strict-Transport-Security: max-age=31536000; includeSubDomains; preload — forca HTTPS por 1 ano"],
    ["JWT assinado HMAC-SHA256", "API REST", "Conteudo do token nao pode ser forjado sem a chave secreta de 256 bits"],
    ["Content-Security-Policy", "Cabecalho HTTP", "default-src 'self' — previne carregamento de recursos de origens nao autorizadas"],
    ["CORS restritivo", "Cabecalho HTTP", "Origens permitidas via variavel CITYORBIT_CORS_ORIGINS — sem wildcard (*)"],
  ],
  [2800, 2600, 4600]
));

children.push(h2("4.2 Dados em Repouso (Data at Rest)"));
children.push(makeTable(
  ["Mecanismo", "Onde Aplicado", "Detalhes Tecnicos"],
  [
    ["BCrypt fator 12", "Senhas de usuarios", "~300ms por operacao de hash — resistente a ataques de dicionario e GPU"],
    ["Segredos via variaveis de ambiente", "JWT secret, senhas, DB password", "Nenhum segredo hardcoded no codigo ou application.properties commitado"],
    ["Senha do banco H2", "Banco de dados", "Variavel CITYORBIT_DB_PASS — sem acesso anonimo ao banco"],
    ["Segredos TOTP criptografados", "TB_MFA_SECRET", "Em producao: criptografar coluna com AES-256 via AttributeConverter ou AWS KMS"],
    ["Criptografia em producao", "Banco persistente", "PostgreSQL com pgcrypto ou AWS RDS com encryption at rest (AES-256)"],
  ],
  [2800, 2600, 4600]
));

children.push(h2("4.3 Cabecalhos de Seguranca HTTP (OWASP Secure Headers)"));
children.push(makeTable(
  ["Cabecalho", "Valor Configurado", "Protecao"],
  [
    ["Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload", "Forca HTTPS; previne downgrade attack"],
    ["Content-Security-Policy", "default-src 'self'; script-src 'self' cdn.jsdelivr.net unpkg.com cdnjs.cloudflare.com", "Previne XSS e carregamento de scripts maliciosos"],
    ["X-Frame-Options", "SAMEORIGIN", "Previne clickjacking via iframe"],
    ["X-Content-Type-Options", "nosniff", "Previne MIME-type sniffing"],
    ["Referrer-Policy", "strict-origin-when-cross-origin", "Controla vazamento de URL para terceiros"],
    ["Permissions-Policy", "geolocation=(), microphone=(), camera=()", "Desabilita APIs sensiveis de navegador"],
  ],
  [3400, 3600, 3000]
));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 5. SEGURANÇA DA INFRAESTRUTURA
// ═══════════════════════════════════════════════════════════════════
children.push(h1("5. Seguranca da Infraestrutura"));
children.push(bodyPara(
  "A infraestrutura do CityOrbit e protegida por uma cadeia de filtros Spring Security, implementando os principios de Zero Trust Architecture conforme NIST SP 800-207."
));

children.push(h2("5.1 Arquitetura Zero Trust"));
children.push(bodyPara(
  "Principios Zero Trust aplicados no CityOrbit:"
));
for (const b of [
  "Never trust, always verify: cada requisicao valida o JWT independentemente, sem confiar em sessoes ou localizacao de rede",
  "Sessao stateless: o servidor nao mantem estado de autenticacao — sem cookies de sessao",
  "Acesso minimo: cada role tem exatamente as permissoes necessarias para sua funcao (principio do menor privilegio)",
  "Monitoramento continuo: AuditLogFilter registra 100% das requisicoes com IP, usuario e resultado",
]) children.push(bullet(b));

children.push(h2("5.2 Cadeia de Filtros de Seguranca"));
children.push(...codeBlock([
  "Ordem de execucao em cada requisicao HTTP:",
  "",
  "1. RateLimitFilter        -> bloqueia IPs com excesso de requisicoes (HTTP 429)",
  "   |  Leitura: 120 req/min por IP",
  "   |  Escrita: 30 req/min por IP (POST/PUT/DELETE/PATCH)",
  "",
  "2. SecurityHeadersFilter  -> injeta HSTS, CSP, X-Frame-Options em todas as respostas",
  "",
  "3. JwtAuthFilter          -> valida assinatura HMAC-SHA256 + consulta blacklist",
  "   |  Token na blacklist: HTTP 401 imediato",
  "   |  Token valido: extrai username e role para o contexto",
  "",
  "4. Spring Security RBAC   -> aplica regras de autorizacao por role",
  "",
  "5. AuditLogFilter         -> registra ip, user, metodo, path, status, tempo (ms)",
  "   |  Respostas 4xx/5xx: gera AUDIT-ALERT automaticamente",
]));

children.push(h2("5.3 Rate Limiting por IP"));
children.push(makeTable(
  ["Tipo de Operacao", "Metodos HTTP", "Limite por IP", "Resposta ao Exceder"],
  [
    ["Leitura", "GET, HEAD, OPTIONS", "120 requisicoes/minuto", "HTTP 429 Too Many Requests + JSON de erro"],
    ["Escrita", "POST, PUT, DELETE, PATCH", "30 requisicoes/minuto", "HTTP 429 Too Many Requests + JSON de erro"],
  ],
  [2800, 2800, 2800, 3600]
));
children.push(...spacer(1));
children.push(noteBox(
  "Nota de producao: em alta escala, usar Redis + Nginx limit_req_zone ou AWS WAF para distribuicao do rate limiting entre multiplas instancias da aplicacao.",
  AMBER
));

children.push(h2("5.4 Monitoramento e Rastreabilidade"));
children.push(bodyPara("O AuditLogFilter gera entradas estruturadas para CADA requisicao:"));
children.push(...codeBlock([
  "[AUDIT] 2026-06-03T21:34:40Z | ip=192.168.1.100 | user=admin | POST /api/cities | status=201 | 45ms",
  "[AUDIT] 2026-06-03T21:34:41Z | ip=10.0.0.5 | user=anonymous | POST /api/auth/login | status=401 | 12ms",
  "[AUDIT-ALERT] POST /api/auth/login -> 401 | ip=10.0.0.5 | user=anonymous",
]));
children.push(...spacer(1));
children.push(bodyPara(
  "Em producao: encaminhar logs para sistema imutavel (AWS CloudWatch, Elastic Stack / ELK) com retencao minima de 90 dias (LGPD Art.46) e 1 ano para logs de seguranca (ISO 27001 A.8.15)."
));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 6. ALINHAMENTO ISO 27001
// ═══════════════════════════════════════════════════════════════════
children.push(h1("6. Alinhamento Normativo — ISO/IEC 27001:2022"));
children.push(bodyPara(
  "O CityOrbit foi projetado com base nos controles da ISO/IEC 27001:2022, adotando o ciclo de gestao de riscos: Identificacao -> Analise (STRIDE) -> Avaliacao (nivel de risco) -> Tratamento (controles) -> Monitoramento (audit log)."
));
children.push(...spacer(1));
children.push(makeTable(
  ["Controle ISO 27001", "Descricao", "Implementacao no CityOrbit", "Arquivo de Evidencia"],
  [
    ["A.5.15", "Controle de acesso", "RBAC com ADMIN e VIEWER; regras por endpoint e por metodo HTTP", "SecurityConfig.java"],
    ["A.5.16", "Gerenciamento de identidade", "InMemoryUserDetailsManager + BCrypt(12); MFA TOTP persistido", "SecurityConfig.java, MfaService.java"],
    ["A.5.17", "Informacoes de autenticacao", "BCrypt(12) para senhas; TOTP para MFA; JWT HMAC-SHA256 para sessao", "JwtUtil.java, MfaService.java"],
    ["A.5.26", "Resposta a incidentes", "Plano completo em 3 fases implementado como API funcional", "SecurityIncidentController.java"],
    ["A.5.29", "Continuidade de seguranca", "Estado do incidente persistido em banco; restauracao automatica no restart (@PostConstruct)", "SecurityIncidentController.java"],
    ["A.5.34", "Privacidade e protecao de dados", "Endpoints LGPD para acesso, exclusao e consentimento persistidos no banco", "LgpdController.java"],
    ["A.8.5", "Autenticacao segura", "Login em 2 fatores (senha + TOTP); expiracao de sessao 8h; aviso MFA para ADMIN", "AuthController.java, MfaController.java"],
    ["A.8.8", "Gerenciamento de vulnerabilidades", "HSTS, CSP, X-Frame-Options, rate limiting, validacao de entrada (Bean Validation)", "SecurityHeadersFilter.java"],
    ["A.8.15", "Log de eventos", "100% das requisicoes auditadas com IP, usuario autenticado e status de resposta", "AuditLogFilter.java"],
    ["A.8.16", "Monitoramento de atividades", "Alertas automaticos em respostas 4xx/5xx no audit log; notificacoes de incidente persistidas", "AuditLogFilter.java"],
    ["A.8.20", "Seguranca de rede", "CORS restritivo por lista de origens; Rate Limiting por IP; HTTPS obrigatorio", "SecurityConfig.java, RateLimitFilter.java"],
    ["A.8.24", "Uso de criptografia", "HMAC-SHA256 (JWT), BCrypt(12) (senhas), TLS 1.2+ (transito), AES-256 (prod/repouso)", "JwtUtil.java, SecurityConfig.java"],
  ],
  [1600, 2600, 4400, 2400]
));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 7. PRIVACIDADE / LGPD
// ═══════════════════════════════════════════════════════════════════
children.push(h1("7. Privacidade e LGPD (Lei 13.709/2018)"));
children.push(bodyPara(
  "O CityOrbit implementa conformidade com a Lei Geral de Protecao de Dados (LGPD) atraves de endpoints dedicados, registro persistente de consentimento e mecanismos de comunicacao de incidentes conforme exigido pela ANPD."
));

children.push(h2("7.1 Base Legal para Tratamento de Dados"));
children.push(makeTable(
  ["Dado Tratado", "Base Legal (LGPD Art. 7)", "Finalidade", "Retencao"],
  [
    ["Username + hash da senha (BCrypt)", "Contrato / Legitimo interesse", "Autenticacao e autorizacao", "Enquanto conta ativa"],
    ["Logs de auditoria (IP, username, timestamp)", "Obrigacao legal (Art. 7, II)", "Rastreabilidade e seguranca", "90 dias operacional / 1 ano seguranca"],
    ["Segredo TOTP do MFA", "Contrato", "Segundo fator de autenticacao", "Enquanto MFA ativo"],
    ["Registro de consentimento", "Consentimento (Art. 7, I)", "Comprovacao do consentimento do titular", "Prazo legal apos revogacao"],
    ["Dados de cidades (coordenadas)", "Legitimo interesse", "Simulacao de impacto climatico", "Enquanto cidade cadastrada"],
  ],
  [3000, 2800, 2600, 1600]
));

children.push(h2("7.2 Direitos dos Titulares Implementados (LGPD Cap. III)"));
children.push(makeTable(
  ["Artigo LGPD", "Direito", "Endpoint", "Metodo", "Persistencia"],
  [
    ["Art. 18, I",  "Confirmacao de existencia de dados", "GET /api/lgpd/my-data",        "GET",    "Consulta banco"],
    ["Art. 18, II", "Acesso aos dados",                   "GET /api/lgpd/my-data",        "GET",    "Consulta banco"],
    ["Art. 18, VI", "Eliminacao dos dados",               "DELETE /api/lgpd/my-data",     "DELETE", "Registra em TB_LGPD_CONSENT"],
    ["Art. 8, §5",  "Registro de consentimento",          "POST /api/lgpd/consent",       "POST",   "Persiste em TB_LGPD_CONSENT"],
    ["Art. 8, §5",  "Revogacao de consentimento",         "POST /api/lgpd/consent",       "POST",   "Persiste em TB_LGPD_CONSENT"],
    ["Art. 9, I",   "Aviso de privacidade",               "GET /api/lgpd/privacy-notice", "GET",    "Retorno estatico"],
  ],
  [1600, 2400, 3200, 1200, 2600]
));

children.push(h2("7.3 Minimizacao e Pseudonimizacao"));
for (const b of [
  "Minimizacao (Art. 6, III): o sistema coleta apenas username (pseudonimo) e hash de senha — sem e-mail, CPF ou dados biometricos",
  "Coordenadas geograficas sao de CIDADES (entidades publicas), nao de pessoas fisicas",
  "Logs de auditoria usam o username autenticado — em producao, aplicar pseudonimizacao apos 90 dias (substituir por hash SHA-256)",
  "Consentimento registrado com IP de origem e timestamp para fins de auditoria",
]) children.push(bullet(b));

children.push(h2("7.4 Comunicacao de Incidentes — LGPD Art. 48"));
children.push(bodyPara(
  "Em caso de incidente de seguranca com potencial impacto a titulares de dados, o CityOrbit aciona automaticamente o IncidentNotificationService que persiste e registra notificacoes para todos os canais configurados:"
));
children.push(...codeBlock([
  "POST /api/security/incident/recover",
  "Corpo: {",
  "  \"notes\": \"Erradicacao confirmada\",",
  "  \"authorizedBy\": \"admin\",",
  "  \"personalDataAffected\": true    <- aciona notificacao ANPD automaticamente",
  "}",
  "",
  "-> IncidentNotificationService.notifyRecovery(..., personalDataAffected=true)",
  "-> Notificacao ANPD persistida em TB_INCIDENT_NOTIFICATION",
  "-> Log: [ANPD-NOTIFY] Notificacao formal pendente para: incidentes@anpd.gov.br",
  "-> Prazo LGPD Art. 48: 72 horas para envio formal a ANPD e titulares afetados",
]));
children.push(pageBreak());

// ═══════════════════════════════════════════════════════════════════
// 8. PLANO DE RESPOSTA A INCIDENTES
// ═══════════════════════════════════════════════════════════════════
children.push(h1("8. Plano de Resposta a Incidentes"));
children.push(bodyPara(
  "Base normativa: ISO 27001 A.5.26 | NIST SP 800-61 Rev.2 | LGPD Art. 48"
));
children.push(bodyPara(
  "O CityOrbit implementa um Plano de Resposta a Incidentes completo e funcional, com as tres fases do ciclo NIST SP 800-61 (Contencao, Erradicacao e Recuperacao) operacionalizadas como endpoints REST protegidos por autenticacao ADMIN. O estado do incidente e persistido no banco de dados (TB_INCIDENT_EVENT), garantindo continuidade mesmo apos reinicializacao do servidor."
));

children.push(h2("8.1 Gatilhos de Acionamento"));
children.push(bodyPara("O plano deve ser acionado ao detectar qualquer um dos seguintes eventos:"));
for (const b of [
  "Mais de 50 falhas de autenticacao em 5 minutos originadas de um mesmo IP",
  "Token JWT com assinatura invalida detectado mais de 10 vezes consecutivas",
  "Acesso ao banco de dados fora do horario operacional configurado",
  "Resposta anomala da NASA POWER API (dados climaticos inconsistentes ou adulterados)",
  "Alerta de sistema de deteccao de intrusao (IDS) externo",
  "Rate limiting ativado persistentemente no mesmo IP por mais de 30 minutos",
]) children.push(bullet(b));

children.push(h2("8.2 FASE 1 — CONTENCAO (Containment)"));
children.push(noteBox("Objetivo: Impedir que o ataque cause mais danos. Tempo alvo: < 1 minuto.", GREEN));
children.push(...spacer(1));
children.push(makeTable(
  ["Passo", "Acao", "Executor", "Endpoint / Ferramenta"],
  [
    ["1", "Confirmar o incidente (verdadeiro positivo)", "Administrador", "Revisar AuditLog"],
    ["2", "REVOGAR TODOS os tokens JWT — desconectar todos os usuarios", "Administrador", "POST /api/security/incident/contain"],
    ["3", "Registrar IP(s) suspeitos identificados nos logs", "Administrador", "GET /api/security/incident/audit-events"],
    ["4", "Bloquear IP(s) no firewall/WAF externo", "Infra", "Firewall / AWS WAF"],
    ["5", "Notificar equipe de seguranca — automatico via IncidentNotificationService", "Sistema", "Automatico ao chamar /contain"],
  ],
  [600, 4000, 2000, 3400]
));
children.push(...spacer(1));
children.push(bodyPara("Exemplo de chamada ao endpoint de contencao:"));
children.push(...codeBlock([
  "POST /api/auth/login  -> obter token ADMIN",
  "",
  "POST /api/security/incident/contain",
  "Authorization: Bearer <token>",
  "Content-Type: application/json",
  "",
  "{",
  "  \"reason\": \"Acesso nao autorizado detectado — multiplas tentativas de forca bruta\",",
  "  \"reportedBy\": \"admin\"",
  "}",
  "",
  "Resposta:",
  "{",
  "  \"fase\": \"CONTENCAO\",",
  "  \"status\": \"LOCKDOWN_ATIVO\",",
  "  \"eventId\": 1,",
  "  \"mensagem\": \"Todos os tokens JWT foram revogados. Estado persistido no banco.\"",
  "}",
]));

children.push(h2("8.3 FASE 2 — ERRADICACAO (Eradication)"));
children.push(noteBox("Objetivo: Remover a causa raiz. Tempo alvo: 30 minutos a 4 horas.", AMBER));
children.push(...spacer(1));
children.push(makeTable(
  ["Passo", "Acao", "Executor", "Detalhes"],
  [
    ["1", "Analisar logs de auditoria completos", "Analista de Seguranca", "GET /api/security/incident/audit-events"],
    ["2", "Identificar o vetor de ataque", "Analista", "Correlacao de logs — IP, usuario, endpoint, horario"],
    ["3", "Rotacionar segredos: chave JWT, senhas de usuarios", "Administrador", "Alterar CITYORBIT_JWT_SECRET e CITYORBIT_ADMIN_PASS"],
    ["4", "Desativar e reemitir segredos TOTP comprometidos", "Administrador", "DELETE /api/auth/mfa/disable"],
    ["5", "Aplicar patch na vulnerabilidade explorada", "Dev", "Correcao de codigo + novo deploy"],
    ["6", "Registrar causa raiz e acoes realizadas", "Administrador", "POST /api/security/incident/eradicate"],
  ],
  [600, 3200, 2000, 4200]
));

children.push(h2("8.4 FASE 3 — RECUPERACAO (Recovery)"));
children.push(noteBox("Objetivo: Restaurar operacao normal com seguranca aumentada. Tempo alvo: 2 a 24 horas.", GREEN));
children.push(...spacer(1));
children.push(makeTable(
  ["Passo", "Acao", "Executor", "Detalhes"],
  [
    ["1", "Verificar integridade dos dados", "DBA / Analista", "Comparar simulacoes e cidades com ultimo backup"],
    ["2", "ENCERRAR LOCKDOWN — permitir novos logins com novas credenciais", "Administrador", "POST /api/security/incident/recover"],
    ["3", "Ativar monitoramento reforcado por 72 horas", "Infra", "Alertas com limiar reduzido"],
    ["4", "Comunicar titulares afetados se dados pessoais foram expostos", "DPO", "Prazo LGPD Art.48: 72h — automatico com personalDataAffected=true"],
    ["5", "Gerar Post-Incident Report", "Equipe", "GET /api/security/incident/status + /notifications"],
    ["6", "Revisar e atualizar politicas de seguranca", "CISO", "Incorporar licoes aprendidas no SECURITY.md"],
  ],
  [600, 3200, 2000, 4200]
));
children.push(...spacer(1));
children.push(bodyPara("Exemplo de chamada ao endpoint de recuperacao:"));
children.push(...codeBlock([
  "POST /api/security/incident/recover",
  "Authorization: Bearer <token>",
  "Content-Type: application/json",
  "",
  "{",
  "  \"notes\": \"Erradicacao confirmada. Credenciais rotacionadas. Patch aplicado.\",",
  "  \"authorizedBy\": \"admin\",",
  "  \"monitoring\": \"ELEVADO_72H\",",
  "  \"personalDataAffected\": false",
  "}",
  "",
  "Resposta:",
  "{",
  "  \"fase\": \"RECUPERACAO\",",
  "  \"status\": \"OPERACAO_NORMAL_RESTAURADA\",",
  "  \"duracaoMinutos\": 47,",
  "  \"rtoCumprido\": true",
  "}",
]));

children.push(h2("8.5 Metricas de Resiliencia (RTO / RPO)"));
children.push(makeTable(
  ["Metrica", "Definicao", "Meta", "Como e atingida"],
  [
    ["RTO", "Recovery Time Objective — tempo para restaurar a operacao", "< 4 horas", "Lockdown em < 1 min + rotacao de credenciais + deploy rapido"],
    ["RPO", "Recovery Point Objective — ponto de dados mais antigo aceitavel", "< 1 hora", "Snapshots periodicos do banco + logs de auditoria imutaveis"],
    ["MTTD", "Mean Time to Detect — tempo medio para detectar o incidente", "< 5 minutos", "AuditLogFilter com alertas automaticos em respostas 4xx"],
    ["MTTC", "Mean Time to Contain — tempo medio para conter o incidente", "< 1 minuto", "Endpoint /contain revoga todos os JWTs instantaneamente"],
  ],
  [1200, 2800, 1600, 4400]
));

children.push(h2("8.6 Fluxo de Decisao do Incidente"));
children.push(...codeBlock([
  "  Alerta detectado (IDS / AuditLogFilter / monitoramento externo)",
  "          |",
  "          v",
  "  Falso positivo? ——Sim——> Documentar e fechar",
  "          |Nao",
  "          v",
  "  POST /api/security/incident/contain",
  "  (LOCKDOWN — Fase 1 — todos os JWTs revogados)",
  "          |",
  "          v",
  "  Analisar GET /api/security/incident/audit-events",
  "  Identificar causa raiz",
  "          |",
  "          v",
  "  POST /api/security/incident/eradicate",
  "  (Registrar erradicacao — Fase 2)",
  "          |",
  "          v",
  "  Erradicacao confirmada?",
  "          |Sim",
  "          v",
  "  POST /api/security/incident/recover",
  "  (Restaurar — Fase 3)",
  "          |",
  "          v",
  "  personalDataAffected=true?",
  "  ——Sim——> Notificacao ANPD automatica (LGPD Art.48 — 72h)",
  "          |",
  "          v",
  "  Post-Incident Report + atualizacao de politicas",
]));

children.push(h2("8.7 Persistencia e Restauracao de Estado"));
children.push(bodyPara(
  "Uma caracteristica critica do plano e a PERSISTENCIA DO ESTADO: todos os eventos de incidente sao gravados na tabela TB_INCIDENT_EVENT do banco de dados H2. Ao reiniciar o servidor durante um incidente ativo, o metodo @PostConstruct do SecurityIncidentController consulta o ultimo evento no banco e, se o incidente ainda estiver em fase de CONTENCAO ou ERRADICACAO, reativa o lockdown automaticamente — garantindo continuidade operacional conforme ISO 27001 A.5.29."
));
children.push(...codeBlock([
  "@PostConstruct",
  "void restoreStateFromDatabase() {",
  "    eventRepository.findFirstByOrderByTimestampDesc().ifPresent(latest -> {",
  "        if (\"CONTAINMENT\".equals(latest.getPhase()) || \"ERADICATION\".equals(latest.getPhase())) {",
  "            tokenBlacklist.revokeAll(\"Restauracao de estado apos reinicializacao\");",
  "            log.error(\"[INCIDENT-RESTORE] Estado restaurado: {} | inicio: {}\",",
  "                latest.getPhase(), latest.getTimestamp());",
  "        }",
  "    });",
  "}",
]));

// ── ASSINATURA FINAL ───────────────────────────────────────────────
children.push(...spacer(4));
children.push(new Paragraph({
  children: [new TextRun({ text: "Arthur Bueno de Oliveira  |  RM 558396", size: 22, color: BLACK, font: "Arial" })],
  alignment: AlignmentType.CENTER, spacing: { before: 80, after: 80 }
}));
children.push(new Paragraph({
  children: [new TextRun({ text: "Joao Vitor Carotta Ribeiro  |  RM 555187", size: 22, color: BLACK, font: "Arial" })],
  alignment: AlignmentType.CENTER, spacing: { before: 80, after: 80 }
}));
children.push(new Paragraph({
  children: [new TextRun({ text: "Victor Magdaleno Marcos  |  RM 556729", size: 22, color: BLACK, font: "Arial" })],
  alignment: AlignmentType.CENTER, spacing: { before: 80, after: 80 }
}));
children.push(...spacer(1));
children.push(new Paragraph({
  children: [new TextRun({ text: "FIAP  |  Engenharia de Software 3ES  |  Cybersecurity  |  Global Solution 2026", size: 20, color: BLACK, font: "Arial", italics: true })],
  alignment: AlignmentType.CENTER, spacing: { before: 80, after: 80 }
}));

// ══════════════════════════════════════════════════════════════════
// MONTAR DOCUMENTO
// ══════════════════════════════════════════════════════════════════
const doc = new Document({
  numbering: {
    config: [{
      reference: "bullets",
      levels: [{
        level: 0, format: LevelFormat.BULLET, text: "•",
        alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } }
      }]
    }]
  },
  styles: {
    default: { document: { run: { font: "Arial", size: 22, color: BLACK } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 36, bold: true, font: "Arial", color: NAVY },
        paragraph: { spacing: { before: 400, after: 200 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 28, bold: true, font: "Arial", color: CYAN },
        paragraph: { spacing: { before: 280, after: 140 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 24, bold: true, font: "Arial", color: BLACK },
        paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 2 } },
    ]
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
      }
    },
    children
  }]
});

Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync(OUTPUT, buffer);
  console.log("DOCX Cybersecurity gerado:", OUTPUT);
}).catch(err => {
  console.error("Erro:", err.message);
  process.exit(1);
});
