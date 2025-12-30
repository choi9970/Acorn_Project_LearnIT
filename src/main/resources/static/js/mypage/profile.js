// 프로필 페이지 JavaScript

let skillChart = null;

document.addEventListener('DOMContentLoaded', function() {
    const analyzeBtn = document.getElementById('analyze-github-btn');
    
    if (analyzeBtn) {
        analyzeBtn.addEventListener('click', function() {
            analyzeGitHub();
        });
    }
    
    // 페이지 로드 시 Thymeleaf에서 전달된 데이터가 있으면 먼저 표시
    if (window.savedSkillChart && window.savedSkillChart.skillNames && window.savedSkillChart.skillNames.length > 0) {
        console.log('Thymeleaf 데이터로 차트 표시:', window.savedSkillChart);
        displaySkillChart(window.savedSkillChart);
        if (window.savedAnalysis) {
            displayAnalysisResult(window.savedAnalysis);
            const resultEl = document.getElementById('github-analysis-result');
            const emptyEl = document.getElementById('github-empty');
            if (resultEl && emptyEl) {
                resultEl.style.display = 'block';
                emptyEl.style.display = 'none';
            }
        }
    } else {
        // 저장된 분석 결과가 없으면 API 호출
        loadSavedAnalysis();
    }
});

/**
 * 저장된 GitHub 분석 결과 로드
 */
async function loadSavedAnalysis() {
    try {
        const response = await fetch('/api/mypage/github/analysis');
        const data = await response.json();
        
        if (data.success && data.analysis) {
            displayAnalysisResult(data.analysis);
            displaySkillChart(data.skillChart);
            
            // 결과 표시
            const resultEl = document.getElementById('github-analysis-result');
            const emptyEl = document.getElementById('github-empty');
            if (resultEl && emptyEl) {
                resultEl.style.display = 'block';
                emptyEl.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('저장된 분석 결과 로드 실패:', error);
        // 에러가 발생해도 계속 진행 (분석하기 버튼은 활성화)
    }
}

/**
 * GitHub 분석 요청
 */
async function analyzeGitHub() {
    const loadingEl = document.getElementById('github-loading');
    const resultEl = document.getElementById('github-analysis-result');
    const emptyEl = document.getElementById('github-empty');
    const analyzeBtn = document.getElementById('analyze-github-btn');
    
    // UI 상태 변경
    loadingEl.style.display = 'block';
    resultEl.style.display = 'none';
    emptyEl.style.display = 'none';
    analyzeBtn.disabled = true;
    analyzeBtn.textContent = '분석 중...';
    
    try {
        const response = await fetch('/api/mypage/github/analyze');
        const data = await response.json();
        
        if (data.success) {
            displayAnalysisResult(data.analysis);
            displaySkillChart(data.skillChart);
            
            resultEl.style.display = 'block';
            emptyEl.style.display = 'none';
        } else {
            // Rate limit 오류인 경우 특별 처리
            if (data.errorType === 'RATE_LIMIT') {
                alert('GitHub API 요청 한도가 초과되었습니다.\n\n잠시 후 다시 시도해주세요.\n(인증된 요청을 사용하면 더 높은 한도를 사용할 수 있습니다.)');
            } else {
                alert('분석 실패: ' + (data.error || '알 수 없는 오류'));
            }
            emptyEl.style.display = 'block';
            emptyEl.textContent = data.error || '분석에 실패했습니다.';
        }
    } catch (error) {
        console.error('GitHub 분석 오류:', error);
        alert('분석 중 오류가 발생했습니다: ' + error.message);
        emptyEl.style.display = 'block';
        emptyEl.textContent = '분석 중 오류가 발생했습니다.';
    } finally {
        loadingEl.style.display = 'none';
        analyzeBtn.disabled = false;
        analyzeBtn.textContent = '다시 분석하기';
    }
}

/**
 * 현재 데이터로 활동량 계산
 */
function calculateActivityLevel(analysis) {
    const commits = analysis.totalCommits || 0;
    const repos = analysis.totalRepos || 0;
    
    if (commits === 0 || repos === 0) {
        return { level: '시작', description: '활동 없음' };
    }
    
    // 실제 분석한 레포지토리 수 (최대 10개)
    const analyzedRepos = Math.min(repos, 10);
    
    // 레포지토리당 평균 커밋 수 (활동의 깊이) - 실제 분석한 레포지토리 수로 계산
    const avgCommitsPerRepo = commits / analyzedRepos;
    
    // 활동량 점수 계산
    let activityScore = 0;
    let factors = [];
    
    // 1. 커밋 수 (50% 가중치) - 기준 상향 조정
    if (commits >= 500) {
        activityScore += 50;
        factors.push('매우 높은 커밋 수');
    } else if (commits >= 200) {
        activityScore += 40;
        factors.push('높은 커밋 수');
    } else if (commits >= 100) {
        activityScore += 30;
        factors.push('적당한 커밋 수');
    } else if (commits >= 50) {
        activityScore += 20;
        factors.push('보통 커밋 수');
    } else {
        activityScore += 10;
        factors.push('낮은 커밋 수');
    }
    
    // 2. 레포지토리당 평균 커밋 수 (30% 가중치) - 활동의 깊이
    if (avgCommitsPerRepo >= 30) {
        activityScore += 30;
        factors.push('매우 깊은 참여');
    } else if (avgCommitsPerRepo >= 20) {
        activityScore += 25;
        factors.push('깊은 참여');
    } else if (avgCommitsPerRepo >= 10) {
        activityScore += 20;
        factors.push('적당한 참여');
    } else if (avgCommitsPerRepo >= 5) {
        activityScore += 15;
        factors.push('보통 참여');
    } else {
        activityScore += 10;
        factors.push('낮은 참여');
    }
    
    // 3. 레포지토리 다양성 (20% 가중치) - 점수 하향 조정
    if (repos >= 30) {
        activityScore += 20;
        factors.push('매우 다양한 프로젝트');
    } else if (repos >= 20) {
        activityScore += 15;
        factors.push('다양한 프로젝트');
    } else if (repos >= 10) {
        activityScore += 12;
        factors.push('여러 프로젝트');
    } else if (repos >= 5) {
        activityScore += 8;
        factors.push('적당한 프로젝트');
    } else {
        activityScore += 5;
        factors.push('소수 프로젝트');
    }
    
    // 활동량 등급 결정 (100점 만점)
    let level, description;
    if (activityScore >= 80) {
        level = '매우 활발';
        description = '지속적이고 깊은 개발 활동';
    } else if (activityScore >= 60) {
        level = '활발';
        description = '규칙적인 개발 활동';
    } else if (activityScore >= 40) {
        level = '보통';
        description = '일반적인 개발 활동';
    } else if (activityScore >= 20) {
        level = '소극적';
        description = '가끔 개발 활동';
    } else {
        level = '시작';
        description = '개발 활동 시작';
    }
    
    return {
        level: level,
        description: description,
        score: activityScore,
        avgCommitsPerRepo: Math.round(avgCommitsPerRepo * 10) / 10,
        factors: factors
    };
}

/**
 * 분석 결과 표시
 */
function displayAnalysisResult(analysis) {
    // 통계 표시
    document.getElementById('stat-repos').textContent = analysis.totalRepos || 0;
    
    // 활동량 계산
    const activity = calculateActivityLevel(analysis);
    const commits = analysis.totalCommits || 0;
    
    // 개발 활동 표시
    const activityText = `${commits} 커밋 · ${activity.level}`;
    const statForksEl = document.getElementById('stat-forks');
    statForksEl.textContent = activityText;
    
    // 커스텀 툴팁 설정
    const tooltipEl = document.getElementById('activity-tooltip');
    if (tooltipEl) {
        const tooltipContent = tooltipEl.querySelector('.tooltip-content');
        tooltipContent.innerHTML = `
            <div class="tooltip-title">${activity.description}</div>
            <div class="tooltip-detail">레포지토리당 평균 ${activity.avgCommitsPerRepo}개 커밋</div>
            <div class="tooltip-factors">${activity.factors.join(' · ')}</div>
        `;
        
        // 마우스 이벤트로 툴팁 표시/숨김
        statForksEl.addEventListener('mouseenter', function() {
            tooltipEl.style.display = 'block';
        });
        
        statForksEl.addEventListener('mouseleave', function() {
            tooltipEl.style.display = 'none';
        });
        
        statForksEl.style.cursor = 'help';
    }
    
    document.getElementById('stat-language').textContent = analysis.mostUsedLanguage || '-';
    
    // 언어 목록 표시
    const languageListEl = document.getElementById('language-list');
    languageListEl.innerHTML = '';
    
    if (analysis.languageStats && Object.keys(analysis.languageStats).length > 0) {
        // 언어별 사용량을 내림차순으로 정렬
        const sortedLanguages = Object.entries(analysis.languageStats)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 10); // 상위 10개만 표시
        
        sortedLanguages.forEach(([language, bytes]) => {
            const languageItem = document.createElement('div');
            languageItem.className = 'language-item';
            
            // 바이트를 KB 또는 MB로 변환
            let size = '';
            if (bytes >= 1024 * 1024) {
                size = (bytes / (1024 * 1024)).toFixed(2) + ' MB';
            } else if (bytes >= 1024) {
                size = (bytes / 1024).toFixed(2) + ' KB';
            } else {
                size = bytes + ' bytes';
            }
            
            languageItem.innerHTML = `
                <span class="language-name">${language}</span>
                <span class="language-size">${size}</span>
            `;
            
            languageListEl.appendChild(languageItem);
        });
    }
}

/**
 * 스킬 차트 표시
 */
function displaySkillChart(skillChartData) {
    console.log('차트 데이터:', skillChartData);
    console.log('언어 목록:', skillChartData?.skillNames);
    console.log('스킬 레벨:', skillChartData?.skillLevels);
    
    const containerEl = document.getElementById('skill-chart-container');
    const emptyEl = document.getElementById('skill-chart-empty');
    const canvasEl = document.getElementById('skill-chart');
    
    if (!skillChartData || !skillChartData.skillNames || skillChartData.skillNames.length === 0) {
        console.warn('차트 데이터가 없습니다:', skillChartData);
        containerEl.style.display = 'none';
        emptyEl.style.display = 'block';
        return;
    }
    
    containerEl.style.display = 'block';
    emptyEl.style.display = 'none';
    
    // 기존 차트가 있으면 제거
    if (skillChart) {
        skillChart.destroy();
    }
    
    // 새로운 차트 생성 (Radar Chart - 육각형)
    const ctx = canvasEl.getContext('2d');
    skillChart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: skillChartData.skillNames,
            datasets: [{
                label: '스킬 레벨',
                data: skillChartData.skillLevels,
                backgroundColor: 'rgba(10, 74, 122, 0.2)',
                borderColor: 'rgba(10, 74, 122, 1)',
                borderWidth: 2,
                pointBackgroundColor: 'rgba(10, 74, 122, 1)',
                pointBorderColor: '#fff',
                pointHoverBackgroundColor: '#fff',
                pointHoverBorderColor: 'rgba(10, 74, 122, 1)'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                r: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        stepSize: 20,
                        font: {
                            size: 12
                        }
                    },
                    pointLabels: {
                        font: {
                            size: 14,
                            weight: 'bold'
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.1)'
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return context.label + ': ' + context.parsed.r.toFixed(1) + '%';
                        }
                    }
                }
            }
        }
    });
}

