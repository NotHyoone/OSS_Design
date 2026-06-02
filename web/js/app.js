/**
 * GitHub Activity Insight – app.js
 *
 * 이 파일은 프론트엔드 뼈대(skeleton)입니다.
 * 실제 백엔드 API와 연결하기 위해 아래 TODO 주석을 구현하세요.
 *
 * 페이지별 진입점:
 *   - initHomePage()     → index.html
 *   - initProgressPage() → progress.html
 *   - initResultPage()   → result.html
 *   - initHistoryPage()  → history.html
 */

'use strict';

/* ================================================================
   공통 유틸리티
   ================================================================ */

/**
 * URL 쿼리 파라미터 파싱
 * @returns {URLSearchParams}
 */
function getParams() {
  return new URLSearchParams(window.location.search);
}

/**
 * GitHub ID 형식 검증 (영문·숫자·하이픈, 1-39자, 하이픈 연속·앞뒤 불가)
 * @param {string} id
 * @returns {boolean}
 */
function isValidGithubId(id) {
  return /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,37}[a-zA-Z0-9])?$/.test(id);
}

/**
 * 날짜 포맷 (YYYY-MM-DD)
 * @param {string|Date} value
 * @returns {string}
 */
function formatDate(value) {
  const d = new Date(value);
  if (isNaN(d)) return '--';
  return d.toISOString().slice(0, 10);
}

/**
 * 증감 방향 클래스
 * @param {number} delta
 * @returns {string}
 */
function deltaClass(delta) {
  if (delta > 0) return 'delta--up';
  if (delta < 0) return 'delta--down';
  return 'delta--neutral';
}

/**
 * 증감 표시 문자열
 * @param {number} delta
 * @returns {string}
 */
function deltaLabel(delta) {
  if (delta > 0) return `+${delta}`;
  return `${delta}`;
}

/* ================================================================
   API 클라이언트 스텁
   (실제 구현 시 fetch 또는 axios 등으로 교체)
   ================================================================ */

const API = {
  /**
   * GitHub ID 존재 여부 확인 (UC-01 Step 3)
   * GET /api/github/validate?id={githubId}
   * @param {string} githubId
   * @returns {Promise<{valid: boolean, avatarUrl?: string}>}
   */
  validateGithubId: async (githubId) => {
    const res = await fetch(`/api/github/validate?id=${encodeURIComponent(githubId)}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },

  /**
   * 분석 요청 생성 (UC-02)
   * POST /api/analysis/request  body: { githubId }
   * @param {string} githubId
   * @returns {Promise<{requestId: string, estimatedSeconds: number}>}
   */
  createAnalysisRequest: async (githubId) => {
    const res = await fetch('/api/analysis/request', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ githubId }),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },

  /**
   * 분석 진행 상태 조회 (UC-03 ~ UC-05 Progress)
   * GET /api/analysis/status/{requestId}
   * @param {string} requestId
   * @returns {Promise<{step: 1|2|3, stepStatus: 'running'|'done'|'error', overallPct: number, detail?: string}>}
   */
  getAnalysisStatus: async (requestId) => {
    const res = await fetch(`/api/analysis/status/${encodeURIComponent(requestId)}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },

  /**
   * 분석 결과 조회 (UC-06)
   * GET /api/analysis/result/{githubId}
   * @param {string} githubId
   * @returns {Promise<AnalysisResult>}
   */
  getAnalysisResult: async (githubId) => {
    const res = await fetch(`/api/analysis/result/${encodeURIComponent(githubId)}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },

  /**
   * 분석 결과 조회 - 요청 ID 기준
   * GET /api/analysis/result/request/{requestId}
   * @param {string} requestId
   * @returns {Promise<AnalysisResult>}
   */
  getAnalysisResultByRequest: async (requestId, accessToken = null) => {
    const tokenQuery = accessToken ? `?token=${encodeURIComponent(accessToken)}` : '';
    const res = await fetch(`/api/analysis/result/request/${encodeURIComponent(requestId)}${tokenQuery}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },

  /**
   * 분석 이력 목록 조회 (UC-08)
   * GET /api/analysis/history/{githubId}
   * @param {string} githubId
   * @returns {Promise<AnalysisResult[]>}
   */
  getAnalysisHistory: async (githubId) => {
    const res = await fetch(`/api/analysis/history/${encodeURIComponent(githubId)}`);
    if (res.status === 204) return [];
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },

  /* ── 취소 ── */
  cancelAnalysis: async (requestId, accessToken = null) => {
    const tokenQuery = accessToken ? `?token=${encodeURIComponent(accessToken)}` : '';
    await fetch(`/api/analysis/cancel/${encodeURIComponent(requestId)}${tokenQuery}`, { method: 'POST' });
  },

  /** 현재 로그인 사용자 조회 (UC-00) */
  getMe: async () => {
    const res = await fetch('/auth/me');
    if (!res.ok) return { loggedIn: false };
    return res.json();
  },

  /** 로그아웃 */
  logout: async () => {
    await fetch('/auth/logout', { method: 'POST' });
  },
};

/* ================================================================
   (하위 호환을 위해 남겨둔 빈 블록 – 실제 데이터는 위 API 객체 사용)
   ================================================================ */

/* ================================================================
   인증 UI 공통 – Auth 상태에 따라 헤더 nav 렌더링
   ================================================================ */

async function initAuthNav() {
  const authNav = document.getElementById('auth-nav');
  if (!authNav) return;
  try {
    const me = await API.getMe();
    if (me.loggedIn) {
      authNav.innerHTML = `
        <span class="nav-user">
          <img src="${escapeHtml(me.avatarUrl || '')}" alt="" class="nav-avatar" />
          <span>${escapeHtml(me.displayName || me.githubId)}</span>
        </span>
        <button class="nav-link nav-logout-btn" id="logout-btn">로그아웃</button>`;
      document.getElementById('logout-btn').addEventListener('click', async () => {
        await API.logout();
        location.reload();
      });
    } else {
      authNav.innerHTML = `<a href="login.html" class="nav-link nav-login-link">로그인</a>`;
    }
  } catch (_) {
    authNav.innerHTML = `<a href="login.html" class="nav-link nav-login-link">로그인</a>`;
  }
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}


/* ================================================================
   HOME PAGE (index.html) – UC-01, UC-02
   ================================================================ */

function initHomePage() {
  const form       = document.getElementById('github-form');
  const input      = document.getElementById('github-id');
  const startBtn   = document.getElementById('start-btn');
  const errorEl    = document.getElementById('github-id-error');
  const statusEl   = document.getElementById('input-status');

  if (!form) return;

  let debounceTimer = null;
  let validating    = false;

  /* 입력 이벤트 – 실시간 형식 검증 (UC-01 Step 2) */
  input.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    const val = input.value.trim();

    // 빈 입력
    if (!val) {
      setInputStatus('idle');
      startBtn.disabled = true;
      return;
    }

    // 형식 오류 (UC-01 2a)
    if (!isValidGithubId(val)) {
      showError('영문·숫자·하이픈(-)만 사용 가능하며, 하이픈이 앞뒤에 올 수 없습니다.');
      setInputStatus('error');
      startBtn.disabled = true;
      return;
    }

    hideError();
    setInputStatus('loading');

    /* 디바운스 300ms 후 API 확인 (UC-01 Step 3) */
    debounceTimer = setTimeout(async () => {
      if (validating) return;
      validating = true;
      try {
        const { valid } = await API.validateGithubId(val);
        if (valid) {
          setInputStatus('ok');
          startBtn.disabled = false;
        } else {
          showError('GitHub에서 해당 ID를 찾을 수 없습니다. <a href="https://github.com" target="_blank" rel="noopener">GitHub에서 확인</a>');
          setInputStatus('error');
          startBtn.disabled = true;
        }
      } catch {
        showError('네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
        setInputStatus('error');
        startBtn.disabled = true;
      } finally {
        validating = false;
      }
    }, 300);
  });

  /* 폼 제출 – 분석 요청 생성 (UC-02) */
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const githubId = input.value.trim();
    if (!githubId || startBtn.disabled) return;

    startBtn.disabled = true;
    startBtn.textContent = '요청 생성 중...';

    try {
      const { requestId, resultAccessToken } = await API.createAnalysisRequest(githubId);
      const tokenQuery = resultAccessToken ? `&token=${encodeURIComponent(resultAccessToken)}` : '';
      // Progress 페이지로 이동
      location.href = `progress.html?id=${encodeURIComponent(githubId)}&req=${encodeURIComponent(requestId)}${tokenQuery}`;
    } catch {
      showError('분석 요청 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.');
      startBtn.disabled = false;
      startBtn.textContent = 'Start Analysis';
    }
  });

  /* 헬퍼 */
  function setInputStatus(state) {
    const map = { idle: '', loading: '⏳', ok: '✅', error: '❌' };
    statusEl.textContent = map[state] ?? '';
  }
  function showError(msg) {
    errorEl.innerHTML = msg;
    errorEl.classList.remove('hidden');
  }
  function hideError() {
    errorEl.textContent = '';
    errorEl.classList.add('hidden');
  }
}

/* ================================================================
   PROGRESS PAGE (progress.html) – UC-03, UC-04, UC-05
   ================================================================ */

function initProgressPage() {
  const usernameEl = document.getElementById('progress-username');
  const requestIdEl = document.getElementById('request-id');
  const overallBar  = document.getElementById('overall-bar');
  const overallPct  = document.getElementById('overall-percent');
  const avatarImg   = document.getElementById('avatar-img');
  const cancelBtn   = document.getElementById('cancel-btn');

  if (!usernameEl) return;

  const params    = getParams();
  const githubId  = params.get('id') || 'unknown';
  const requestId = params.get('req') || 'REQ-0000';
  const accessToken = params.get('token');

  /* 초기 UI 설정 */
  usernameEl.textContent = '@' + githubId;
  requestIdEl.textContent = requestId;
  if (avatarImg) avatarImg.src = `https://avatars.githubusercontent.com/${githubId}`;

  const STEP_IDS = ['step-collect', 'step-metrics', 'step-score'];

  let pollTimer = null;

  /* 폴링 시작 */
  pollTimer = setInterval(async () => {
    try {
      const status = await API.getAnalysisStatus(requestId);
      updateProgress(status);

      if (status.stepStatus === 'done' && status.step === 3 && status.overallPct >= 100) {
        clearInterval(pollTimer);
        // 결과 페이지로 이동
        setTimeout(() => {
          const tokenQuery = accessToken ? `&token=${encodeURIComponent(accessToken)}` : '';
          location.href = `result.html?id=${encodeURIComponent(githubId)}&req=${encodeURIComponent(requestId)}${tokenQuery}`;
        }, 800);
      }

      if (status.stepStatus === 'error') {
        clearInterval(pollTimer);
        showError('GitHub API 요청 중 오류가 발생했습니다. 다시 시도해 주세요.');
      }
    } catch {
      /* 네트워크 오류 – 폴링 유지 */
    }
  }, 2000);

  /* 오류 상태 표시 */
  function showError(message) {
    const errorEl = document.getElementById('error-notice');
    const msgEl = document.getElementById('error-message');
    if (errorEl && msgEl) {
      msgEl.textContent = message;
      errorEl.classList.remove('hidden');
    }
    if (cancelBtn) cancelBtn.hidden = true;
  }

  /* 취소 버튼 (UC-02 1a.3) */
  if (cancelBtn) {
    cancelBtn.addEventListener('click', () => {
      clearInterval(pollTimer);
      API.cancelAnalysis(requestId, accessToken).catch(() => {});
      location.href = 'index.html';
    });
  }

  /**
   * @param {{step: number, stepStatus: string, overallPct: number, detail?: string}} status
   */
  function updateProgress({ step, stepStatus, overallPct: pct, detail }) {
    /* 전체 프로그레스바 */
    overallBar.style.width = pct + '%';
    overallBar.setAttribute('aria-valuenow', Math.round(pct));
    overallPct.textContent = Math.round(pct) + '%';

    /* 각 단계 아이템 업데이트 */
    STEP_IDS.forEach((id, idx) => {
      const el         = document.getElementById(id);
      const badge      = el.querySelector('.step-status-badge');
      const spinner    = el.querySelector('.step-spinner');
      const detailEl   = el.querySelector('[id$="-detail"]');
      const stepNum    = idx + 1;

      el.classList.remove('step--pending', 'step--running', 'step--done', 'step--error');

      if (stepNum < step) {
        el.classList.add('step--done');
        badge.textContent = '완료';
        spinner.classList.add('hidden');
      } else if (stepNum === step) {
        if (stepStatus === 'running') {
          el.classList.add('step--running');
          badge.textContent = '진행 중';
          spinner.classList.remove('hidden');
          if (detail && detailEl) detailEl.textContent = detail;
        } else if (stepStatus === 'done') {
          el.classList.add('step--done');
          badge.textContent = '완료';
          spinner.classList.add('hidden');
        } else if (stepStatus === 'error') {
          el.classList.add('step--error');
          badge.textContent = '오류';
          spinner.classList.add('hidden');
        }
      } else {
        el.classList.add('step--pending');
        badge.textContent = '대기';
        spinner.classList.add('hidden');
      }
    });
  }

  function showRateLimitNotice(show) {
    const el = document.getElementById('rate-limit-notice');
    if (el) el.hidden = !show;
  }
}

/* ================================================================
   RESULT PAGE (result.html) – UC-06, UC-07
   ================================================================ */

function initResultPage() {
  const resultUsernameEl = document.getElementById('result-username');
  if (!resultUsernameEl) return;

  const params   = getParams();
  const githubId = params.get('id') || 'username';
  const requestId = params.get('req');
  const accessToken = params.get('token');

  /* 이력 비교 버튼 (UC-08) */
  const historyBtn = document.getElementById('btn-history');
  if (historyBtn) {
    historyBtn.addEventListener('click', () => {
      location.href = `history.html?id=${encodeURIComponent(githubId)}`;
    });
  }

  /* 결과 데이터 로드 */
  const loadResult = requestId
    ? API.getAnalysisResultByRequest(requestId, accessToken)
    : API.getAnalysisResult(githubId);

  loadResult.then((result) => {
    renderResult(result);
  }).catch((error) => {
    showError('분석 결과를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요.');
  });

  /* 오류 상태 표시 */
  function showError(message) {
    const errorEl = document.getElementById('error-notice');
    const msgEl = document.getElementById('error-message');
    if (errorEl && msgEl) {
      msgEl.textContent = message;
      errorEl.classList.remove('hidden');
    }
    const resultSection = document.querySelector('.result-header');
    if (resultSection) resultSection.classList.add('hidden');
  }

  /* PDF 다운로드 버튼 (UC-07) */
  const pdfBtn = document.getElementById('btn-pdf');
  if (pdfBtn) {
    pdfBtn.addEventListener('click', async () => {
      pdfBtn.disabled = true;
      pdfBtn.textContent = '다운로드 중...';
      try {
        const tokenQuery = accessToken ? `?token=${encodeURIComponent(accessToken)}` : '';
        const reportUrl = requestId
          ? `/api/analysis/report/request/${encodeURIComponent(requestId)}${tokenQuery}`
          : `/api/analysis/report/${encodeURIComponent(githubId)}`;
        const response = await fetch(reportUrl);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${githubId}_Analysis_${formatDate(new Date()).replace(/-/g, '')}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      } catch (error) {
        alert('PDF 다운로드에 실패했습니다. 잠시 후 다시 시도해 주세요.');
      } finally {
        pdfBtn.disabled = false;
        pdfBtn.textContent = '⬇️ PDF 다운로드';
      }
    });
  }

  /**
   * 결과 데이터를 DOM에 렌더링
   * @param {object} result
   */
  function renderResult(result) {
    /* 헤더 */
    document.getElementById('result-username').textContent = '@' + result.githubId;
    document.getElementById('developer-type').textContent  = result.developerType;
    const avatarEl = document.getElementById('result-avatar');
    if (avatarEl) avatarEl.src = result.avatarUrl;

    /* 낮은 신뢰도 경고 (UC-06 4-7b) */
    if (result.trustLevel === 'LOW' || result.trustLevel === 'PARTIAL') {
      document.getElementById('trust-warning')?.classList.remove('hidden');
    }

    /* 게이지 */
    animateGauge(result.totalScore);
    const scoreTextEl = document.getElementById('gauge-score-text');
    if (scoreTextEl) scoreTextEl.textContent = result.totalScore;

    document.getElementById('analysis-date').textContent = formatDate(result.analysisDate);
    document.getElementById('trust-level').textContent   = result.trustLevel;
    document.getElementById('score-summary-text').textContent = result.summaryText;

    /* 지표 바 */
    const metricsMap = [
      { key: 'activity',  barClass: '.metric-bar--activity',  scoreId: 'score-activity',  descId: 'desc-activity' },
      { key: 'diversity', barClass: '.metric-bar--diversity', scoreId: 'score-diversity', descId: 'desc-diversity' },
      { key: 'collab',    barClass: '.metric-bar--collab',    scoreId: 'score-collab',    descId: 'desc-collab' },
      { key: 'persist',   barClass: '.metric-bar--persist',   scoreId: 'score-persist',   descId: 'desc-persist' },
    ];
    metricsMap.forEach(({ key, barClass, scoreId, descId }) => {
      const m = result.metrics[key];
      if (!m) return;
      const bar = document.querySelector(barClass);
      if (bar) {
        setTimeout(() => {
          bar.style.width = m.score + '%';
          bar.setAttribute('aria-valuenow', m.score);
        }, 200);
      }
      const scoreEl = document.getElementById(scoreId);
      if (scoreEl) scoreEl.textContent = m.score;
      const descEl = document.getElementById(descId);
      if (descEl && m.desc) descEl.textContent = m.desc;
    });

    /* 강점 (UC-06 Step 6) */
    const strengthsList = document.getElementById('strengths-list');
    if (strengthsList && result.strengths?.length) {
      strengthsList.innerHTML = result.strengths.map(
        (s) => `<li class="feedback-item">${escapeHtml(s)}</li>`
      ).join('');
    }

    /* 개선 영역 (UC-06 Step 7) */
    const improvList = document.getElementById('improvements-list');
    if (improvList && result.improvements?.length) {
      improvList.innerHTML = result.improvements.map(
        (s) => `<li class="feedback-item">${escapeHtml(s)}</li>`
      ).join('');
    }
  }

  /**
   * SVG 게이지 애니메이션
   * @param {number} score 0-100
   */
  function animateGauge(score) {
    const fill = document.getElementById('gauge-fill');
    if (!fill) return;
    const maxDash = 283; // arc length
    const offset  = maxDash - (maxDash * score / 100);
    setTimeout(() => { fill.style.strokeDashoffset = offset; }, 100);
  }
}

/* ================================================================
   HISTORY PAGE (history.html) – UC-08
   ================================================================ */

function initHistoryPage() {
  const compareControls = document.getElementById('compare-controls');
  if (!compareControls) return;

  const baseSelect    = document.getElementById('base-select');
  const compareSelect = document.getElementById('compare-select');
  const compareBtn    = document.getElementById('compare-btn');
  const compareResult = document.getElementById('compare-result');
  const historyList   = document.getElementById('history-list');
  const emptyState    = document.getElementById('empty-state');

  let historyData = [];

  /* GitHub ID 취득 – URL 파라미터 또는 현재 로그인 사용자 */
  let githubId = getParams().get('id');

  async function loadHistoryData() {
    if (!githubId) {
      try {
        const me = await API.getMe();
        if (!me.loggedIn) {
          showError('로그인이 필요합니다.');
          return;
        }
        githubId = me.githubId;
      } catch {
        showError('사용자 정보를 불러올 수 없습니다.');
        return;
      }
    }

    /* 이력 데이터 로드 */
    API.getAnalysisHistory(githubId).then((history) => {
      historyData = history;

      // 이력 2건 미만 – 빈 상태 (UC-08 Preconditions)
      if (history.length < 2) {
        compareControls.classList.add('hidden');
        emptyState?.classList.remove('hidden');
        return;
      }

      /* 셀렉트 옵션 생성 */
      history.forEach((item, idx) => {
        const label = `${formatDate(item.analysisDate)} (점수: ${item.totalScore})`;
        baseSelect.add(new Option(label, idx));
        compareSelect.add(new Option(label, idx));
      });
      /* 기본: 첫 번째 vs 마지막 */
      baseSelect.value    = '0';
      compareSelect.value = String(history.length - 1);
      compareBtn.disabled = false;

      /* 이력 목록 렌더링 */
      renderHistoryList(history);
    }).catch((error) => {
      showError('분석 이력을 불러올 수 없습니다. 잠시 후 다시 시도해 주세요.');
    });
  }

  loadHistoryData();

  /* 오류 상태 표시 */
  function showError(message) {
    const errorEl = document.getElementById('error-notice');
    const msgEl = document.getElementById('error-message');
    if (errorEl && msgEl) {
      msgEl.textContent = message;
      errorEl.classList.remove('hidden');
    }
    compareControls.classList.add('hidden');
    historyList.classList.add('hidden');
  }

  /* 셀렉트 변경 – 같은 시점 선택 방지 */
  [baseSelect, compareSelect].forEach((sel) => {
    sel.addEventListener('change', () => {
      compareBtn.disabled = (baseSelect.value === compareSelect.value) || !baseSelect.value || !compareSelect.value;
    });
  });

  /* 비교하기 버튼 (UC-08 Step 5-9) */
  compareBtn.addEventListener('click', () => {
    const baseIdx    = Number(baseSelect.value);
    const currentIdx = Number(compareSelect.value);
    const base    = historyData[baseIdx];
    const current = historyData[currentIdx];
    if (!base || !current) return;

    renderCompareResult(base, current);
    compareResult.classList.remove('hidden');
    compareResult.scrollIntoView({ behavior: 'smooth' });
  });

  /**
   * 비교 결과 렌더링 (UC-08 Step 6-10)
   */
  function renderCompareResult(base, current) {
    /* 요약 카드 */
    document.getElementById('base-date').textContent    = formatDate(base.analysisDate);
    document.getElementById('base-score').textContent   = base.totalScore;
    document.getElementById('base-type').textContent    = base.developerType || '--';
    document.getElementById('current-date').textContent = formatDate(current.analysisDate);
    document.getElementById('current-score').textContent = current.totalScore;
    document.getElementById('current-type').textContent  = current.developerType || '--';

    /* 비교 테이블 (UC-08 Step 7) */
    const rows = [
      { label: '종합 점수',   baseVal: base.totalScore, curVal: current.totalScore },
      { label: '활동성',      baseVal: base.metrics.activity?.score,  curVal: current.metrics.activity?.score },
      { label: '기술 다양성', baseVal: base.metrics.diversity?.score, curVal: current.metrics.diversity?.score },
      { label: '협업도',      baseVal: base.metrics.collab?.score,    curVal: current.metrics.collab?.score },
      { label: '지속성',      baseVal: base.metrics.persist?.score,   curVal: current.metrics.persist?.score },
    ];

    const tbody = document.getElementById('compare-tbody');
    tbody.innerHTML = rows.map(({ label, baseVal, curVal }) => {
      const b = baseVal ?? 0;
      const c = curVal  ?? 0;
      const delta = c - b;
      const pct   = b !== 0 ? ((delta / b) * 100).toFixed(1) : '--';
      const cls   = deltaClass(delta);
      return `<tr>
        <td>${escapeHtml(label)}</td>
        <td>${b}</td>
        <td>${c}</td>
        <td class="${cls}">${deltaLabel(delta)}</td>
        <td class="${cls}">${pct !== '--' ? pct + '%' : pct}</td>
      </tr>`;
    }).join('');

    /* 스파크라인 (UC-08 Step 9) */
    drawSparkline(historyData.map((h) => h.totalScore));

    /* 격려 메시지 (UC-08 Step 10) */
    const delta = current.totalScore - base.totalScore;
    const encourageEl = document.getElementById('encourage-text');
    if (encourageEl) {
      if (delta > 0)      encourageEl.textContent = `이전 분석 대비 ${delta}점 향상되었습니다! 꾸준한 노력이 결실을 맺고 있어요. 계속 성장하세요! 🎉`;
      else if (delta < 0) encourageEl.textContent = `이번에는 ${Math.abs(delta)}점 하락했지만, 분석을 통해 개선 방향을 파악하고 다시 도전해 보세요! 💪`;
      else                encourageEl.textContent = `점수 변화가 없습니다. 유지도 중요하지만 새로운 도전으로 더 성장해 봐요! 🚀`;
    }
  }

  /**
   * 이력 목록 카드 렌더링
   */
  function renderHistoryList(history) {
    if (!historyList) return;
    historyList.innerHTML = history.map((item) => `
      <div class="history-item" role="listitem">
        <div>
          <strong>${formatDate(item.analysisDate)}</strong>
          <span style="color:var(--color-text-muted); margin-left:8px; font-size:.8rem;">${item.developerType || ''}</span>
        </div>
        <div style="display:flex; align-items:center; gap:12px;">
          <span style="font-size:1.3rem; font-weight:800;">${item.totalScore}<span style="font-size:.8rem; color:var(--color-text-muted)"> / 100</span></span>
          <a href="result.html?id=${encodeURIComponent(item.githubId)}&req=${encodeURIComponent(item.requestId)}" class="btn btn-ghost" style="font-size:.78rem; padding:4px 10px;">결과 보기</a>
        </div>
      </div>
    `).join('');
  }

  /**
   * Canvas 스파크라인 (UC-08 Step 9)
   * @param {number[]} scores
   */
  function drawSparkline(scores) {
    const canvas = document.getElementById('sparkline-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const W = canvas.width, H = canvas.height;
    const pts = scores.slice(-5);
    if (pts.length < 2) return;

    const min = Math.max(0,   Math.min(...pts) - 10);
    const max = Math.min(100, Math.max(...pts) + 10);
    const toX = (i) => (i / (pts.length - 1)) * (W - 40) + 20;
    const toY = (v) => H - 10 - ((v - min) / (max - min)) * (H - 20);

    ctx.clearRect(0, 0, W, H);

    /* Gradient fill */
    const grad = ctx.createLinearGradient(0, 0, 0, H);
    grad.addColorStop(0, 'rgba(88,166,255,.35)');
    grad.addColorStop(1, 'rgba(88,166,255,0)');

    ctx.beginPath();
    pts.forEach((v, i) => i === 0 ? ctx.moveTo(toX(i), toY(v)) : ctx.lineTo(toX(i), toY(v)));
    ctx.lineTo(toX(pts.length - 1), H);
    ctx.lineTo(toX(0), H);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    /* Line */
    ctx.beginPath();
    pts.forEach((v, i) => i === 0 ? ctx.moveTo(toX(i), toY(v)) : ctx.lineTo(toX(i), toY(v)));
    ctx.strokeStyle = 'rgba(88,166,255,1)';
    ctx.lineWidth   = 2;
    ctx.lineJoin    = 'round';
    ctx.stroke();

    /* Dots + labels */
    pts.forEach((v, i) => {
      ctx.beginPath();
      ctx.arc(toX(i), toY(v), 4, 0, Math.PI * 2);
      ctx.fillStyle   = '#58a6ff';
      ctx.strokeStyle = '#0d1117';
      ctx.lineWidth   = 2;
      ctx.fill();
      ctx.stroke();

      ctx.fillStyle  = '#e6edf3';
      ctx.font       = '11px sans-serif';
      ctx.textAlign  = 'center';
      ctx.fillText(v, toX(i), toY(v) - 8);
    });
  }
}

/* ================================================================
   진입점 – 현재 페이지 감지 후 초기화
   ================================================================ */
document.addEventListener('DOMContentLoaded', () => {
  initAuthNav();
  const path = location.pathname;

  if (path.endsWith('index.html') || path.endsWith('/') || path === '') {
    initHomePage();
  } else if (path.endsWith('progress.html')) {
    initProgressPage();
  } else if (path.endsWith('result.html')) {
    initResultPage();
  } else if (path.endsWith('history.html')) {
    initHistoryPage();
  }
});
