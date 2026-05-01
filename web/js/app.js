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
   * TODO: GET /api/github/validate?id={githubId}
   * @param {string} githubId
   * @returns {Promise<{valid: boolean, avatarUrl?: string}>}
   */
  validateGithubId: async (githubId) => {
    // --- STUB ---
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ valid: true, avatarUrl: `https://avatars.githubusercontent.com/${githubId}` });
      }, 800);
    });
  },

  /**
   * 분석 요청 생성 (UC-02)
   * TODO: POST /api/analysis/request  body: { githubId }
   * @param {string} githubId
   * @returns {Promise<{requestId: string, estimatedSeconds: number}>}
   */
  createAnalysisRequest: async (githubId) => {
    // --- STUB ---
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ requestId: 'REQ-' + Date.now(), estimatedSeconds: 30 });
      }, 400);
    });
  },

  /**
   * 분석 진행 상태 조회 (UC-03 ~ UC-05 Progress)
   * TODO: GET /api/analysis/status/{requestId}
   * @param {string} requestId
   * @returns {Promise<{step: 1|2|3, stepStatus: 'running'|'done'|'error', overallPct: number, detail?: string}>}
   */
  getAnalysisStatus: async (requestId) => {
    // --- STUB: 5초마다 step 증가 시뮬레이션 ---
    const elapsed = (Date.now() - Number(requestId.split('-')[1])) / 1000;
    if (elapsed < 5)  return { step: 1, stepStatus: 'running', overallPct: Math.min(30, elapsed * 6), detail: '저장소 목록 조회 중...' };
    if (elapsed < 10) return { step: 2, stepStatus: 'running', overallPct: Math.min(65, 30 + (elapsed - 5) * 7), detail: '지표 계산 중...' };
    if (elapsed < 14) return { step: 3, stepStatus: 'running', overallPct: Math.min(95, 65 + (elapsed - 10) * 7.5), detail: '피드백 생성 중...' };
    return { step: 3, stepStatus: 'done', overallPct: 100, detail: '완료' };
  },

  /**
   * 분석 결과 조회 (UC-06)
   * TODO: GET /api/analysis/result/{githubId}
   * @param {string} githubId
   * @returns {Promise<AnalysisResult>}
   */
  getAnalysisResult: async (githubId) => {
    // --- STUB ---
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          githubId,
          avatarUrl: `https://avatars.githubusercontent.com/${githubId}`,
          analysisDate: new Date().toISOString(),
          developerType: 'Junior Developer',
          totalScore: 72,
          trustLevel: 'HIGH',
          summaryText: `${githubId}님의 GitHub 활동을 분석한 결과, 활동성과 기술 다양성이 양호하며 협업 경험을 더 쌓으면 Senior 레벨로 성장할 수 있습니다.`,
          metrics: {
            activity:  { score: 80, desc: '주 평균 커밋 빈도 우수' },
            diversity: { score: 68, desc: '5개 언어 사용, 다양성 양호' },
            collab:    { score: 55, desc: 'PR/Issue 참여 경험 부족' },
            persist:   { score: 85, desc: '12개월 연속 활동 유지' },
          },
          strengths: [
            '꾸준한 커밋 습관 – 월 평균 40회 이상 커밋',
            '다양한 언어 경험 (Python, JavaScript, Java 등)',
            '지속적인 개인 프로젝트 유지 관리',
          ],
          improvements: [
            'Pull Request를 통한 오픈소스 기여 경험 추가',
            'Issue 코멘트·리뷰 활동으로 협업 이력 강화',
            '공개 저장소에 README 및 문서 보강',
            'Fork 후 개선 사항 반영 PR 도전',
          ],
        });
      }, 600);
    });
  },

  /**
   * 분석 이력 목록 조회 (UC-08)
   * TODO: GET /api/analysis/history/{githubId}
   * @param {string} githubId
   * @returns {Promise<AnalysisResult[]>}
   */
  getAnalysisHistory: async (githubId) => {
    // --- STUB ---
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
          {
            githubId, analysisDate: '2026-03-01T00:00:00Z',
            totalScore: 62, developerType: 'Beginner',
            metrics: { activity: { score: 65 }, diversity: { score: 55 }, collab: { score: 40 }, persist: { score: 70 } },
          },
          {
            githubId, analysisDate: '2026-05-01T00:00:00Z',
            totalScore: 72, developerType: 'Junior Developer',
            metrics: { activity: { score: 80 }, diversity: { score: 68 }, collab: { score: 55 }, persist: { score: 85 } },
          },
        ]);
      }, 600);
    });
  },
};

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
      const { requestId } = await API.createAnalysisRequest(githubId);
      // Progress 페이지로 이동
      location.href = `progress.html?id=${encodeURIComponent(githubId)}&req=${encodeURIComponent(requestId)}`;
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
          location.href = `result.html?id=${encodeURIComponent(githubId)}`;
        }, 800);
      }

      if (status.stepStatus === 'error') {
        clearInterval(pollTimer);
        showRateLimitNotice(true);
      }
    } catch {
      /* 네트워크 오류 – 폴링 유지 */
    }
  }, 2000);

  /* 취소 버튼 (UC-02 1a.3) */
  if (cancelBtn) {
    cancelBtn.addEventListener('click', () => {
      clearInterval(pollTimer);
      // TODO: POST /api/analysis/cancel/{requestId}
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

  /* 결과 데이터 로드 */
  API.getAnalysisResult(githubId).then((result) => {
    renderResult(result);
  }).catch(() => {
    // TODO: 오류 상태 UI 표시
  });

  /* PDF 다운로드 버튼 (UC-07) */
  const pdfBtn = document.getElementById('btn-pdf');
  if (pdfBtn) {
    pdfBtn.addEventListener('click', () => {
      // TODO: POST /api/report/pdf/{githubId}  → blob 다운로드
      alert(`PDF 다운로드 기능은 백엔드 구현 후 활성화됩니다.\n파일명: ${githubId}_Analysis_${formatDate(new Date()).replace(/-/g, '')}.pdf`);
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

  // TODO: 실제 환경에서는 로그인된 사용자 ID를 세션/쿠키에서 읽어옴
  const githubId = getParams().get('id') || 'demo-user';

  let historyData = [];

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
  }).catch(() => {
    // TODO: 오류 상태 UI
  });

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
    historyList.innerHTML = history.map((item, idx) => `
      <div class="history-item" role="listitem">
        <div>
          <strong>${formatDate(item.analysisDate)}</strong>
          <span style="color:var(--color-text-muted); margin-left:8px; font-size:.8rem;">${item.developerType || ''}</span>
        </div>
        <div style="display:flex; align-items:center; gap:12px;">
          <span style="font-size:1.3rem; font-weight:800;">${item.totalScore}<span style="font-size:.8rem; color:var(--color-text-muted)"> / 100</span></span>
          <a href="result.html?id=${encodeURIComponent(item.githubId)}&snap=${idx}" class="btn btn-ghost" style="font-size:.78rem; padding:4px 10px;">결과 보기</a>
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
   공통 – XSS 방어용 HTML 이스케이프
   ================================================================ */
function escapeHtml(str) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/* ================================================================
   진입점 – 현재 페이지 감지 후 초기화
   ================================================================ */
document.addEventListener('DOMContentLoaded', () => {
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
