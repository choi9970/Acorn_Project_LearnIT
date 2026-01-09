(function() {
    // 캘린더 월 이동 변수
    let currentCalendarYear = new Date().getFullYear();
    let currentCalendarMonth = new Date().getMonth() + 1;

    // 캘린더 렌더링 함수
    function renderCalendar(data) {
        if (!data || !data.year || !data.month) {
            const now = new Date();
            data = {
                year: now.getFullYear(),
                month: now.getMonth() + 1,
                days: []
            };
        }
        
        currentCalendarYear = data.year;
        currentCalendarMonth = data.month;
        updateCalendarMonthDisplay();
        
        const calendarEl = document.getElementById('dashboard-calendar');
        if (!calendarEl) {
            console.error('캘린더 요소를 찾을 수 없습니다');
            return;
        }
        
        const year = data.year;
        const month = data.month;
        const days = data.days || [];
        
        // 날짜별 데이터를 맵으로 변환
        const dayMap = new Map();
        days.forEach(day => {
            if (day && day.day) {
                dayMap.set(day.day, day);
            }
        });
        
        // 첫 번째 날짜로 Date 객체 생성
        const firstDay = new Date(year, month - 1, 1);
        const lastDay = new Date(year, month, 0);
        const daysInMonth = lastDay.getDate();
        const startDayOfWeek = firstDay.getDay(); // 0=일요일, 1=월요일...
        
        // 요일 헤더
        const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
        
        let html = '<div class="calendar-header">';
        dayNames.forEach(name => {
            html += '<div class="calendar-day-header">' + name + '</div>';
        });
        html += '</div>';
        
        html += '<div class="calendar-grid">';
        
        // 빈 칸 추가 (첫 주 시작 전)
        for (let i = 0; i < startDayOfWeek; i++) {
            html += '<div class="calendar-day empty"></div>';
        }
        
        // 오늘 날짜 확인
        const today = new Date();
        const isCurrentMonth = today.getFullYear() === year && today.getMonth() + 1 === month;
        
        // 날짜 칸 추가
        for (let day = 1; day <= daysInMonth; day++) {
            const dayData = dayMap.get(day);
            const hasStudy = dayData && (dayData.hasStudy === true || dayData.hasStudy === 1);
            const hasAttendance = dayData && (dayData.hasAttendance === true || dayData.hasAttendance === 1);
            // hasTodo는 Boolean 또는 1/0으로 올 수 있음, 또는 todoCount > 0이면 할일이 있는 것으로 판단
            const todoCount = dayData && dayData.todoCount ? dayData.todoCount : 0;
            const hasTodo = dayData && (dayData.hasTodo === true || dayData.hasTodo === 1 || todoCount > 0);
            const lectureCount = dayData && dayData.lectureCount ? dayData.lectureCount : 0;
            const studyMinutes = dayData && dayData.studyMinutes ? dayData.studyMinutes : 0;
            const isToday = isCurrentMonth && day === today.getDate();
            
            let dayClass = 'calendar-day clickable-day';
            if (isToday) dayClass += ' today';
            if (hasStudy) dayClass += ' has-study';
            if (hasAttendance) dayClass += ' has-attendance';
            if (hasTodo) dayClass += ' has-todo';
            
            // 학습 표시 (할일처럼 초록색 점만 표시 - 강의명은 모달에서 확인)
            let studyHtml = '';
            if (hasStudy) {
                // 학습이 있는 경우 - 초록색 점만 표시 (강의명은 모달에서 확인)
                studyHtml = '<div class="calendar-study-indicator" title="학습 기록 있음"></div>';
            }
            
            // 할일 표시 (구글 캘린더처럼 제목 표시)
            let todoHtml = '';
            if (dayData && dayData.todos && dayData.todos.length > 0) {
                todoHtml = '<div class="calendar-todos">';
                dayData.todos.forEach((todo, index) => {
                    if (index < 3) { // 최대 3개만 표시
                        const todoClass = todo.isCompleted ? 'todo-completed' : '';
                        todoHtml += '<div class="calendar-todo-item ' + todoClass + '" title="' + (todo.title || '') + '">';
                        todoHtml += '<span class="todo-dot">●</span>';
                        todoHtml += '<span class="todo-title">' + (todo.title || '') + '</span>';
                        todoHtml += '</div>';
                    }
                });
                if (dayData.todos.length > 3) {
                    todoHtml += '<div class="calendar-todo-more">+' + (dayData.todos.length - 3) + '개</div>';
                }
                todoHtml += '</div>';
            } else if (hasTodo) {
                // 할일이 있지만 목록이 없는 경우 (호환성)
                todoHtml = '<div class="calendar-todo-indicator" title="할일 ' + todoCount + '개">●</div>';
            }
            
            html += '<div class="' + dayClass + '" data-day="' + day + '" data-year="' + year + '" data-month="' + month + '">';
            html += '<div class="calendar-day-number">' + day + '</div>';
            html += studyHtml;
            html += todoHtml;
            html += '</div>';
        }
        
        html += '</div>';
        
        calendarEl.innerHTML = html;
        bindCalendarDayClickEvents();
    }

    // 캘린더 날짜 클릭 이벤트
    function bindCalendarDayClickEvents() {
        document.querySelectorAll('.clickable-day').forEach(dayEl => {
            dayEl.addEventListener('click', function() {
                const year = parseInt(this.getAttribute('data-year'));
                const month = parseInt(this.getAttribute('data-month'));
                const day = parseInt(this.getAttribute('data-day'));
                
                openDayDetailModal(year, month, day);
            });
        });
    }

    // 캘린더 월 표시 업데이트
    function updateCalendarMonthDisplay() {
        const monthDisplay = document.getElementById('calendar-month-display');
        if (monthDisplay) {
            monthDisplay.textContent = currentCalendarMonth + '월';
        }
    }
    
    // 오늘 날짜의 할일 목록 로드
    function loadTodayTodos() {
        const todayTodosContent = document.getElementById('today-todos-content');
        const todayCompletedSection = document.getElementById('today-completed-section');
        const todayCompletedList = document.getElementById('today-completed-list');
        const todayCompletedCount = document.getElementById('today-completed-count');
        
        if (!todayTodosContent) return;
        
        const today = new Date();
        const year = today.getFullYear();
        const month = today.getMonth() + 1;
        const day = today.getDate();
        
        apiCall(`/mypage/todos/list?year=${year}&month=${month}&day=${day}`, {
            errorMessage: '오늘의 할일 로드 실패'
        })
        .then(todos => {
            if (!todos || todos.length === 0) {
                todayTodosContent.innerHTML = '<p class="empty-message">오늘 할일이 없습니다.</p>';
                if (todayCompletedSection) {
                    todayCompletedSection.style.display = 'none';
                }
                return;
            }
            
            // 미완료 할일과 완료된 할일 분리
            const pendingTodos = todos.filter(todo => !todo.isCompleted);
            const completedTodos = todos.filter(todo => todo.isCompleted);
            
            // 미완료 할일 표시 (최대 5개)
            if (pendingTodos.length === 0) {
                todayTodosContent.innerHTML = '<p class="empty-message">모든 할일을 완료했습니다! 🎉</p>';
            } else {
                let html = '<div class="today-todos-list">';
                pendingTodos.slice(0, 5).forEach(todo => {
                    html += `
                        <div class="today-todo-item" data-todo-id="${todo.todoId || ''}">
                            <input type="checkbox" class="today-todo-checkbox" data-todo-id="${todo.todoId || ''}">
                            <span class="todo-dot-small">●</span>
                            <span class="todo-title-small">${todo.title || ''}</span>
                        </div>
                    `;
                });
                if (pendingTodos.length > 5) {
                    html += `<p class="todo-more-indicator">외 ${pendingTodos.length - 5}개</p>`;
                }
                html += '</div>';
                todayTodosContent.innerHTML = html;
                
                // 체크박스 이벤트 리스너 추가
                bindTodayTodosCheckboxEvents();
            }
            
            // 완료된 할일 표시
            if (completedTodos.length > 0 && todayCompletedSection && todayCompletedList && todayCompletedCount) {
                todayCompletedCount.textContent = `(${completedTodos.length})`;
                
                let completedHtml = '<div class="today-completed-todos-list">';
                completedTodos.forEach(todo => {
                    completedHtml += `
                        <div class="today-todo-item completed" data-todo-id="${todo.todoId || ''}">
                            <input type="checkbox" class="today-todo-checkbox" checked data-todo-id="${todo.todoId || ''}">
                            <span class="todo-dot-small completed-dot">✓</span>
                            <span class="todo-title-small completed-title">${todo.title || ''}</span>
                        </div>
                    `;
                });
                completedHtml += '</div>';
                todayCompletedList.innerHTML = completedHtml;
                
                // 완료된 항목의 체크박스 이벤트 리스너 추가
                bindTodayCompletedTodosCheckboxEvents();
                
                // 완료된 항목 섹션 표시 (기본적으로 접힘 상태)
                todayCompletedSection.style.display = 'block';
                todayCompletedList.style.display = 'none';
                const toggleIcon = document.querySelector('#today-completed-toggle .completed-toggle-icon');
                if (toggleIcon) {
                    toggleIcon.textContent = '▶';
                }
            } else if (todayCompletedSection) {
                todayCompletedSection.style.display = 'none';
            }
        })
        .catch(error => {
            console.error('오늘의 할일 로드 실패:', error);
            todayTodosContent.innerHTML = '<p class="empty-message">할일을 불러올 수 없습니다.</p>';
            if (todayCompletedSection) {
                todayCompletedSection.style.display = 'none';
            }
        });
    }
    
    // 완료된 항목의 체크박스 이벤트 바인딩
    function bindTodayCompletedTodosCheckboxEvents() {
        document.querySelectorAll('#today-completed-list .today-todo-checkbox').forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                const todoId = this.getAttribute('data-todo-id');
                if (!todoId) {
                    console.error('할일 ID가 없습니다.');
                    this.checked = true; // 완료된 항목은 체크 해제 방지
                    return;
                }
                
                const isCompleted = this.checked;
                
                // 완료된 항목의 체크를 해제하면 미완료로 변경
                if (!isCompleted) {
                    completeTodoOnServer(todoId, false)
                        .then(() => {
                            console.log('할일 미완료 처리 성공:', todoId);
                            loadTodayTodos(); // 목록 새로고침
                            if (currentCalendarYear && currentCalendarMonth) {
                                loadCalendarData(currentCalendarYear, currentCalendarMonth);
                            }
                        })
                        .catch(error => {
                            console.error('할일 미완료 처리 실패:', error);
                            this.checked = true; // 체크 상태 되돌리기
                            alert('할일 미완료 처리에 실패했습니다: ' + error.message);
                        });
                }
            });
        });
    }
    
    // 오늘의 할일 체크박스 이벤트 바인딩
    function bindTodayTodosCheckboxEvents() {
        document.querySelectorAll('.today-todo-checkbox').forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                const todoId = this.getAttribute('data-todo-id');
                if (!todoId) {
                    console.error('할일 ID가 없습니다.');
                    this.checked = false;
                    return;
                }
                
                const isCompleted = this.checked;
                const todoItem = this.closest('.today-todo-item');
                
                // 서버에 완료 처리 요청
                completeTodoOnServer(todoId, isCompleted)
                    .then(() => {
                        console.log('할일 완료 처리 성공:', todoId, isCompleted);
                        // 완료된 할일은 목록에서 제거하고 목록 새로고침
                        if (isCompleted) {
                            // 애니메이션 효과를 위해 약간의 딜레이 후 제거
                            todoItem.style.opacity = '0.5';
                            todoItem.style.textDecoration = 'line-through';
                            setTimeout(() => {
                                loadTodayTodos(); // 목록 새로고침
                                // 캘린더도 업데이트
                                if (currentCalendarYear && currentCalendarMonth) {
                                    loadCalendarData(currentCalendarYear, currentCalendarMonth);
                                }
                            }, 300);
                        } else {
                            // 미완료 처리 시 목록 새로고침
                            loadTodayTodos();
                        }
                    })
                    .catch(error => {
                        console.error('할일 완료 처리 실패:', error);
                        this.checked = !isCompleted; // 체크 상태 되돌리기
                        alert('할일 완료 처리에 실패했습니다: ' + error.message);
                    });
            });
        });
    }

    // 페이지 로드 완료 후 실행
    document.addEventListener('DOMContentLoaded', function() {
        try {
            // 주간 학습 초기화 - 현재 주 (offset = 0) 데이터 즉시 로드
            // HTML에 서버 데이터가 있어도 JavaScript로 현재 날짜 기준으로 덮어쓰기
            updateWeeklyLearning(0);
            
            // 오늘의 할일 목록 로드
            loadTodayTodos();
            
            // 오늘의 할일 "더 보기" 버튼 이벤트 리스너
            const todayTodosMoreBtn = document.getElementById('today-todos-more-btn');
            if (todayTodosMoreBtn) {
                todayTodosMoreBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    const today = new Date();
                    openDayDetailModal(today.getFullYear(), today.getMonth() + 1, today.getDate());
                });
            }
            
            // 완료된 항목 토글 버튼 이벤트 리스너
            const todayCompletedToggle = document.getElementById('today-completed-toggle');
            const todayCompletedList = document.getElementById('today-completed-list');
            if (todayCompletedToggle && todayCompletedList) {
                todayCompletedToggle.addEventListener('click', function() {
                    const isExpanded = todayCompletedList.style.display !== 'none';
                    const toggleIcon = todayCompletedToggle.querySelector('.completed-toggle-icon');
                    
                    if (isExpanded) {
                        todayCompletedList.style.display = 'none';
                        if (toggleIcon) toggleIcon.textContent = '▶';
                    } else {
                        todayCompletedList.style.display = 'block';
                        if (toggleIcon) toggleIcon.textContent = '▼';
                    }
                });
            }
            
            // 캘린더 초기화 - 전역 변수에서 캘린더 데이터 가져오기 (HTML에서 설정됨)
            const calendarData = window.dashboardCalendarData || null;
            const calendarEl = document.getElementById('dashboard-calendar');
            
            if (calendarData && calendarData.year && calendarData.month && calendarEl) {
                currentCalendarYear = calendarData.year;
                currentCalendarMonth = calendarData.month;
                updateCalendarMonthDisplay();
                renderCalendar(calendarData);
            } else if (calendarEl) {
                // 데이터가 없어도 기본 캘린더 표시 (현재 월)
                const now = new Date();
                currentCalendarYear = now.getFullYear();
                currentCalendarMonth = now.getMonth() + 1;
                updateCalendarMonthDisplay();
                // 현재 월의 데이터를 서버에서 가져오기
                loadCalendarData(currentCalendarYear, currentCalendarMonth);
            }
        } catch (e) {
            console.error('대시보드 초기화 오류:', e);
        }
    });

    // 초기 캘린더 년/월 설정
    const initialCalendarData = window.dashboardCalendarData || null;
    if (initialCalendarData && initialCalendarData.year && initialCalendarData.month) {
        currentCalendarYear = initialCalendarData.year;
        currentCalendarMonth = initialCalendarData.month;
    } else {
        const now = new Date();
        currentCalendarYear = now.getFullYear();
        currentCalendarMonth = now.getMonth() + 1;
    }
    updateCalendarMonthDisplay();

    // 일일 학습 목표 모달 관련
    const dailyGoalBtn = document.getElementById('daily-goal-btn');
    const dailyGoalModal = document.getElementById('daily-goal-modal');
    const closeModalBtn = document.getElementById('close-modal');
    const saveGoalsBtn = document.getElementById('save-goals-btn');

    // 모달 열기
    if (dailyGoalBtn && dailyGoalModal) {
        dailyGoalBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            dailyGoalModal.style.display = 'flex';
        });
    }

    // 모달 닫기
    if (closeModalBtn && dailyGoalModal) {
        closeModalBtn.addEventListener('click', function() {
            dailyGoalModal.style.display = 'none';
        });
    }

    // 모달 외부 클릭 시 닫기
    if (dailyGoalModal) {
        dailyGoalModal.addEventListener('click', function(e) {
            if (e.target === dailyGoalModal) {
                dailyGoalModal.style.display = 'none';
            }
        });
    }

    // 목표 섹션 토글
    document.querySelectorAll('.goal-toggle').forEach(toggle => {
        toggle.addEventListener('click', function() {
            const goalType = this.getAttribute('data-goal');
            const content = document.getElementById(goalType + '-goal-content');
            const icon = this.querySelector('.toggle-icon');
            
            if (content) {
                if (content.style.display === 'none') {
                    content.style.display = 'block';
                    if (icon) icon.textContent = '▲';
                } else {
                    content.style.display = 'none';
                    if (icon) icon.textContent = '▼';
                }
            }
        });
    });

    // 목표 값 조정
    document.querySelectorAll('.input-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const goalType = this.getAttribute('data-goal');
            const input = document.getElementById(goalType + '-goal-input');
            if (!input) return;
            
            const isPlus = this.classList.contains('plus-btn');
            const isMinus = this.classList.contains('minus-btn');
            let value = parseInt(input.value) || 0;
            const min = parseInt(input.getAttribute('min')) || 0;
            const max = parseInt(input.getAttribute('max')) || 100;
            const step = parseInt(input.getAttribute('step')) || 1;

            if (isPlus && value < max) {
                value = Math.min(value + step, max);
            } else if (isMinus && value > min) {
                value = Math.max(value - step, min);
            }

            input.value = value;
            updateGoalValue(goalType, value);
            updateProgressCircle(goalType, value, max);
        });
    });

    // 입력 필드 직접 변경
    ['class', 'time', 'note'].forEach(goalType => {
        const input = document.getElementById(goalType + '-goal-input');
        if (input) {
            input.addEventListener('change', function() {
                const value = parseInt(this.value) || 0;
                const min = parseInt(this.getAttribute('min')) || 0;
                const max = parseInt(this.getAttribute('max')) || 100;
                const clampedValue = Math.max(min, Math.min(value, max));
                this.value = clampedValue;
                updateGoalValue(goalType, clampedValue);
                updateProgressCircle(goalType, clampedValue, max);
            });
        }
    });

    function updateGoalValue(goalType, value) {
        const valueEl = document.getElementById(goalType + '-goal-value');
        if (valueEl) {
            if (goalType === 'time') {
                valueEl.textContent = '하루 ' + value + '분';
            } else {
                valueEl.textContent = '하루 ' + value + '개';
            }
        }
    }

    function updateProgressCircle(goalType, value, max) {
        const circle = document.querySelector('#' + goalType + '-goal-content .progress-ring');
        if (circle && max > 0) {
            const circumference = 2 * Math.PI * 35;
            const progress = value / max;
            const offset = circumference * (1 - progress);
            circle.style.strokeDashoffset = offset;
        }
    }

    // 저장 버튼
    if (saveGoalsBtn && dailyGoalModal) {
        saveGoalsBtn.addEventListener('click', function() {
            const classInput = document.getElementById('class-goal-input');
            const timeInput = document.getElementById('time-goal-input');
            const noteInput = document.getElementById('note-goal-input');
            
            if (classInput && timeInput && noteInput) {
                const goals = {
                    classGoal: parseInt(classInput.value) || 2,
                    timeGoal: parseInt(timeInput.value) || 10,
                    noteGoal: parseInt(noteInput.value) || 2
                };
                
                // 서버에 저장하는 API 호출
                fetch('/api/mypage/daily-goals', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(goals)
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('서버 응답 오류: ' + response.status);
                    }
                    const contentType = response.headers.get('content-type');
                    if (!contentType || !contentType.includes('application/json')) {
                        throw new Error('서버에서 JSON 응답을 받지 못했습니다.');
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.success) {
                        alert('일일 학습 목표가 저장되었습니다.');
                        dailyGoalModal.style.display = 'none';
                        // 목표 저장 후 주간 학습 데이터 다시 로드하여 목표 반영
                        updateWeeklyLearning(currentWeekOffset);
                    } else {
                        alert('목표 저장에 실패했습니다: ' + (data.error || '알 수 없는 오류'));
                    }
                })
                .catch(error => {
                    console.error('목표 저장 실패:', error);
                    alert('목표 저장에 실패했습니다. 나중에 다시 시도해주세요.');
                });
            }
        });
    }

    // 날짜별 툴팁 표시
    function bindTooltipEvents() {
        document.querySelectorAll('.day-tooltip-trigger').forEach(trigger => {
            const tooltip = trigger.nextElementSibling;
            if (!tooltip || !tooltip.classList.contains('day-tooltip')) return;
            
            trigger.addEventListener('mouseenter', function() {
                tooltip.style.display = 'block';
            });
            
            trigger.addEventListener('mouseleave', function() {
                tooltip.style.display = 'none';
            });
        });
    }
    bindTooltipEvents();

    // 주간 학습 주 이동 기능
    let currentWeekOffset = 0;
    let currentWeekStart = null;

    function getWeekStart(date) {
        const d = new Date(date);
        d.setHours(0, 0, 0, 0); // 시간을 00:00:00으로 설정
        const day = d.getDay(); // 0=일요일, 1=월요일, ..., 6=토요일
        // 월요일이 주의 시작일
        // 월요일(1)부터 토요일(6)까지: day - 1일 전으로 이동
        // 일요일(0)인 경우: -6일 (전 주 월요일로 이동)
        let daysToSubtract = 0;
        if (day === 0) {
            daysToSubtract = 6; // 일요일이면 6일 전 (월요일)
        } else {
            daysToSubtract = day - 1; // 월요일(1)이면 0일, 화요일(2)이면 1일, ..., 토요일(6)이면 5일
        }
        const result = new Date(d);
        result.setDate(d.getDate() - daysToSubtract);
        result.setHours(0, 0, 0, 0);
        return result;
    }

    function updateWeeklyLearning(offset) {
        const now = new Date();
        const targetDate = new Date(now);
        targetDate.setDate(now.getDate() + (offset * 7));
        
        const startOfWeek = getWeekStart(targetDate);
        currentWeekStart = new Date(startOfWeek);
        
        const year = startOfWeek.getFullYear();
        const month = startOfWeek.getMonth() + 1;
        const startDateStr = `${year}-${String(month).padStart(2, '0')}-${String(startOfWeek.getDate()).padStart(2, '0')}`;
        
        // 서버에서 주간 학습 데이터 가져오기
        fetch(`/api/mypage/weekly-learning?year=${year}&month=${month}&startDate=${startDateStr}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('서버 응답 오류: ' + response.status);
                }
                const contentType = response.headers.get('content-type');
                if (!contentType || !contentType.includes('application/json')) {
                    throw new Error('서버에서 JSON 응답을 받지 못했습니다.');
                }
                return response.json();
            })
            .then(data => {
                // data가 객체이고 success 필드가 있는 경우만 체크
                if (data && typeof data === 'object' && data.success === false) {
                    throw new Error(data.error || '주간 학습 데이터를 불러올 수 없습니다.');
                }
                
                const weekLabelEl = document.getElementById('week-label-display');
                if (weekLabelEl && data && data.weekLabel) {
                    weekLabelEl.textContent = data.weekLabel;
                }
                
                // 주간 통계 업데이트 (목표 포함)
                const weeklyStats = document.querySelector('.weekly-stats');
                if (weeklyStats && data.dailyLearnings) {
                    const statItems = weeklyStats.querySelectorAll('.stat-value');
                    const goal = data.goal || {};
                    const classGoal = goal.classGoal || 0;
                    const timeGoal = goal.timeGoal || 0;
                    const interpreterGoal = goal.interpreterGoal || 0;
                    
                    const totalLectures = data.totalLectures || 0;
                    const totalMinutes = data.totalMinutes || 0;
                    const totalNotes = data.totalNotes || 0;
                    
                    // 주간 목표 계산 (일일 목표 * 7일)
                    const weekClassGoal = classGoal * 7;
                    const weekTimeGoal = timeGoal * 7;
                    const weekInterpreterGoal = interpreterGoal * 7;
                    
                    if (statItems.length >= 3) {
                        // 목표가 있으면 "실제 / 목표" 형식으로 표시
                        if (weekClassGoal > 0) {
                            statItems[0].textContent = `${totalLectures} / ${weekClassGoal}개`;
                            statItems[0].classList.add('has-goal');
                            if (totalLectures >= weekClassGoal) {
                                statItems[0].classList.add('goal-achieved');
                            }
                        } else {
                            statItems[0].textContent = totalLectures;
                            statItems[0].classList.remove('has-goal', 'goal-achieved');
                        }
                        
                        if (weekTimeGoal > 0) {
                            statItems[1].textContent = `${totalMinutes} / ${weekTimeGoal}분`;
                            statItems[1].classList.add('has-goal');
                            if (totalMinutes >= weekTimeGoal) {
                                statItems[1].classList.add('goal-achieved');
                            }
                        } else {
                            statItems[1].textContent = totalMinutes + '분';
                            statItems[1].classList.remove('has-goal', 'goal-achieved');
                        }
                        
                        if (weekInterpreterGoal > 0) {
                            statItems[2].textContent = `${totalNotes} / ${weekInterpreterGoal}회`;
                            statItems[2].classList.add('has-goal');
                            if (totalNotes >= weekInterpreterGoal) {
                                statItems[2].classList.add('goal-achieved');
                            }
                        } else {
                            statItems[2].textContent = totalNotes;
                            statItems[2].classList.remove('has-goal', 'goal-achieved');
                        }
                    }
                }
                
                updateWeeklyCalendarFromData(startOfWeek, data);
            })
            .catch(error => {
                console.error('주간 학습 데이터 로드 실패:', error);
                // 에러 시 기본 업데이트
                updateWeeklyCalendar(startOfWeek);
            });
    }

    function updateWeeklyCalendar(startOfWeek) {
        const dayNames = ['월', '화', '수', '목', '금', '토', '일'];
        const weeklyCalendar = document.querySelector('.weekly-calendar');
        if (!weeklyCalendar) return;
        
        // 오늘 날짜 확인
        const today = new Date();
        const todayYear = today.getFullYear();
        const todayMonth = today.getMonth() + 1;
        const todayDay = today.getDate();
        
        let html = '';
        for (let i = 0; i < 7; i++) {
            const date = new Date(startOfWeek);
            date.setDate(startOfWeek.getDate() + i);
            const day = date.getDate();
            const dayOfWeek = dayNames[i];
            const dateYear = date.getFullYear();
            const dateMonth = date.getMonth() + 1;
            
            // 오늘 날짜인지 확인
            const isToday = dateYear === todayYear && dateMonth === todayMonth && day === todayDay;
            
            // 기본값 (서버 데이터가 없을 경우)
            const hasStudy = false;
            const lectureCount = 0;
            const studyMinutes = 0;
            const noteCount = 0;
            
            html += `
                <div class="day-item">
                    <div class="day-name">${dayOfWeek}</div>
                    <div class="day-circle day-tooltip-trigger ${isToday ? 'today' : ''} ${hasStudy ? 'has-study' : ''}" 
                         data-day="${day}"
                         data-day-of-week="${dayOfWeek}"
                         data-lecture-count="${lectureCount}"
                         data-study-minutes="${studyMinutes}"
                         data-note-count="${noteCount}">
                    </div>
                    <div class="day-tooltip">
                        <div class="tooltip-date">${dateYear}. ${dateMonth}. ${day}. ${dayOfWeek}</div>
                        <div class="tooltip-stats">
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon purple">📝</span>
                                <span class="tooltip-label">인터프리터</span>
                                <span class="tooltip-value">${noteCount}회</span>
                            </div>
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon blue">📚</span>
                                <span class="tooltip-label">총 학습</span>
                                <span class="tooltip-value">${studyMinutes}분</span>
                            </div>
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon teal">✅</span>
                                <span class="tooltip-label">완료 수업</span>
                                <span class="tooltip-value">${lectureCount}개</span>
                            </div>
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon orange">▶</span>
                                <span class="tooltip-label">재생 시간</span>
                                <span class="tooltip-value">${studyMinutes}분</span>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }
        weeklyCalendar.innerHTML = html;
        bindTooltipEvents();
    }
    
    function updateWeeklyCalendarFromData(startOfWeek, data) {
        const dayNames = ['월', '화', '수', '목', '금', '토', '일'];
        const weeklyCalendar = document.querySelector('.weekly-calendar');
        if (!weeklyCalendar) return;
        
        // 오늘 날짜 확인
        const today = new Date();
        const todayYear = today.getFullYear();
        const todayMonth = today.getMonth() + 1;
        const todayDay = today.getDate();
        
        let html = '';
        const dailyLearnings = data.dailyLearnings || [];
        
        for (let i = 0; i < 7; i++) {
            const date = new Date(startOfWeek);
            date.setDate(startOfWeek.getDate() + i);
            const day = date.getDate();
            const dayOfWeek = dayNames[i];
            const dateYear = date.getFullYear();
            const dateMonth = date.getMonth() + 1;
            
            // 오늘 날짜인지 확인
            const isToday = dateYear === todayYear && dateMonth === todayMonth && day === todayDay;
            
            // 서버 데이터에서 해당 일자의 데이터 찾기
            const dailyData = dailyLearnings[i] || {};
            const hasStudy = dailyData.hasStudy || false;
            const lectureCount = dailyData.lectureCount || 0;
            const studyMinutes = dailyData.studyMinutes || 0;
            const noteCount = dailyData.noteCount || 0;
            
            // 목표 데이터 가져오기
            const goal = data.goal || {};
            const classGoal = goal.classGoal || 0;
            const timeGoal = goal.timeGoal || 0;
            const interpreterGoal = goal.interpreterGoal || 0;
            
            // 목표 달성 여부
            const classAchieved = classGoal > 0 && lectureCount >= classGoal;
            const timeAchieved = timeGoal > 0 && studyMinutes >= timeGoal;
            const interpreterAchieved = interpreterGoal > 0 && noteCount >= interpreterGoal;
            
            html += `
                <div class="day-item">
                    <div class="day-name">${dayOfWeek}</div>
                    <div class="day-circle day-tooltip-trigger ${isToday ? 'today' : ''} ${hasStudy ? 'has-study' : ''} ${classAchieved && timeAchieved && interpreterAchieved ? 'goal-achieved' : ''}" 
                         data-day="${dailyData.day || day}"
                         data-day-of-week="${dailyData.dayOfWeek || dayOfWeek}"
                         data-lecture-count="${lectureCount}"
                         data-study-minutes="${studyMinutes}"
                         data-note-count="${noteCount}">
                    </div>
                    <div class="day-tooltip">
                        <div class="tooltip-date">${dateYear}. ${dateMonth}. ${day}. ${dayOfWeek}</div>
                        <div class="tooltip-stats">
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon purple">📝</span>
                                <span class="tooltip-label">인터프리터</span>
                                <span class="tooltip-value">${noteCount}${interpreterGoal > 0 ? ` / ${interpreterGoal}` : ''}회${interpreterAchieved ? ' ✓' : ''}</span>
                            </div>
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon blue">📚</span>
                                <span class="tooltip-label">총 학습</span>
                                <span class="tooltip-value">${studyMinutes}${timeGoal > 0 ? ` / ${timeGoal}` : ''}분${timeAchieved ? ' ✓' : ''}</span>
                            </div>
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon teal">✅</span>
                                <span class="tooltip-label">완료 수업</span>
                                <span class="tooltip-value">${lectureCount}${classGoal > 0 ? ` / ${classGoal}` : ''}개${classAchieved ? ' ✓' : ''}</span>
                            </div>
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon orange">▶</span>
                                <span class="tooltip-label">재생 시간</span>
                                <span class="tooltip-value">${studyMinutes}분</span>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }
        weeklyCalendar.innerHTML = html;
        bindTooltipEvents();
    }

    // 전 주 버튼
    const prevWeekBtn = document.getElementById('prev-week-btn');
    if (prevWeekBtn) {
        prevWeekBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            currentWeekOffset--;
            updateWeeklyLearning(currentWeekOffset);
        });
    }

    // 다음 주 버튼
    const nextWeekBtn = document.getElementById('next-week-btn');
    if (nextWeekBtn) {
        nextWeekBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            currentWeekOffset++;
            updateWeeklyLearning(currentWeekOffset);
        });
    }


    // 더미 데이터 생성 함수 제거됨 - 서버에서 데이터를 가져옴

    // 서버에서 캘린더 데이터 가져오기
    function loadCalendarData(year, month) {
        fetch(`/api/mypage/calendar?year=${year}&month=${month}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('서버 응답 오류: ' + response.status);
                }
                const contentType = response.headers.get('content-type');
                if (!contentType || !contentType.includes('application/json')) {
                    throw new Error('서버에서 JSON 응답을 받지 못했습니다.');
                }
                return response.json();
            })
            .then(data => {
                // data가 객체이고 success 필드가 있는 경우만 체크
                if (data && typeof data === 'object' && data.success === false) {
                    throw new Error(data.error || '캘린더 데이터를 불러올 수 없습니다.');
                }
                console.log('캘린더 데이터:', data);
                console.log('할일이 있는 날짜:', data.days ? data.days.filter(d => d.hasTodo || d.todoCount > 0) : []);
                renderCalendar(data);
            })
            .catch(error => {
                console.error('캘린더 데이터 로드 실패:', error);
                // 캘린더 데이터는 서버에서 가져옴
                console.error('캘린더 데이터 로드 실패');
            });
    }

    // 전달 버튼
    const calendarPrevMonthBtn = document.getElementById('calendar-prev-month-btn');
    if (calendarPrevMonthBtn) {
        calendarPrevMonthBtn.addEventListener('click', function() {
            currentCalendarMonth--;
            if (currentCalendarMonth < 1) {
                currentCalendarMonth = 12;
                currentCalendarYear--;
            }
            loadCalendarData(currentCalendarYear, currentCalendarMonth);
        });
    }

    // 다음달 버튼
    const calendarNextMonthBtn = document.getElementById('calendar-next-month-btn');
    if (calendarNextMonthBtn) {
        calendarNextMonthBtn.addEventListener('click', function() {
            currentCalendarMonth++;
            if (currentCalendarMonth > 12) {
                currentCalendarMonth = 1;
                currentCalendarYear++;
            }
            loadCalendarData(currentCalendarYear, currentCalendarMonth);
        });
    }

    // 날짜 상세 모달 열기
    function openDayDetailModal(year, month, day) {
        const modal = document.getElementById('day-detail-modal');
        const dateDisplay = document.getElementById('day-detail-date');
        
        if (modal && dateDisplay) {
            // 날짜와 요일 표시
            const date = new Date(year, month - 1, day);
            const weekdays = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
            const weekday = weekdays[date.getDay()];
            dateDisplay.textContent = `${month}월 ${day}일 ${weekday}`;
            
            // 할일 목록 초기화
            loadDayTodos(year, month, day);
            // 오늘 수강한 강의 목록 로드
            loadDailyCourses(year, month, day);
            
            modal.style.display = 'flex';
        }
    }

    // 날짜 상세 모달 닫기
    const dayDetailModal = document.getElementById('day-detail-modal');
    if (dayDetailModal) {
        dayDetailModal.addEventListener('click', function(e) {
            if (e.target === dayDetailModal || e.target.id === 'day-detail-menu-btn') {
                dayDetailModal.style.display = 'none';
            }
        });
    }

    // 할일 추가 버튼 클릭
    const showTodoInputBtn = document.getElementById('show-todo-input-btn');
    const todoInputSection = document.getElementById('todo-input-section');
    const newTodoTitle = document.getElementById('new-todo-title');
    
    if (showTodoInputBtn && todoInputSection) {
        showTodoInputBtn.addEventListener('click', function() {
            todoInputSection.style.display = todoInputSection.style.display === 'none' ? 'block' : 'none';
            if (newTodoTitle) {
                newTodoTitle.focus();
            }
        });
    }

    // 할일 저장 함수 (공통 로직)
    function saveTodoFromInput() {
        if (!newTodoTitle) {
            console.error('할일 입력 필드(new-todo-title)를 찾을 수 없습니다.');
            return;
        }
        
        const text = newTodoTitle.value.trim();
        console.log('할일 저장 함수 호출, 입력된 텍스트:', text);
        
        if (!text) {
            console.log('입력된 텍스트가 비어있습니다.');
            alert('할일 제목을 입력해주세요.');
            return;
        }
        
        // 날짜 정보 가져오기
        const dateDisplay = document.getElementById('day-detail-date');
        let year = currentCalendarYear || new Date().getFullYear();
        let month = new Date().getMonth() + 1;
        let day = new Date().getDate();
        
        if (dateDisplay) {
            const dateText = dateDisplay.textContent;
            console.log('날짜 표시 텍스트:', dateText);
            const match = dateText.match(/(\d+)월 (\d+)일/);
            if (match) {
                month = parseInt(match[1]);
                day = parseInt(match[2]);
                year = currentCalendarYear || new Date().getFullYear();
            }
        }
        
        const targetDate = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        console.log('할일 저장 시작:', { text, targetDate, year, month, day });
        
        // 서버에 저장
        saveTodoToServer(text, targetDate, false)
            .then(savedTodo => {
                console.log('할일 저장 응답:', savedTodo);
                if (savedTodo && savedTodo.todoId) {
                    addTodoItem(text, false, savedTodo.todoId);
                    newTodoTitle.value = '';
                    if (todoInputSection) {
                        todoInputSection.style.display = 'none';
                    }
                                // 할일 저장 후 캘린더 업데이트 및 오늘의 할일 목록 새로고침
                                if (currentCalendarYear && currentCalendarMonth) {
                                    loadCalendarData(currentCalendarYear, currentCalendarMonth);
                                }
                                loadTodayTodos();
                } else {
                    console.error('할일 저장 응답에 todoId가 없습니다:', savedTodo);
                    alert('할일 추가에 실패했습니다. (todoId 없음)');
                }
            })
            .catch(error => {
                console.error('할일 추가 실패:', error);
                alert('할일 추가에 실패했습니다: ' + error.message);
            });
    }

    // 할일 추가 (Enter 키)
    if (newTodoTitle) {
        console.log('할일 입력 필드 이벤트 리스너 등록 완료');
        newTodoTitle.addEventListener('keypress', function(e) {
            console.log('키 입력 이벤트:', e.key);
            if (e.key === 'Enter') {
                e.preventDefault();
                saveTodoFromInput();
            }
        });
    } else {
        console.error('할일 입력 필드(new-todo-title)를 찾을 수 없습니다.');
    }

    // 할일 저장 버튼 클릭
    const saveTodoBtn = document.getElementById('save-todo-btn');
    if (saveTodoBtn) {
        saveTodoBtn.addEventListener('click', function(e) {
            e.preventDefault();
            saveTodoFromInput();
        });
    }

    function addTodoItem(text, isCompleted = false, todoId = null) {
        const pendingList = document.getElementById('pending-todo-list');
        const completedList = document.getElementById('completed-todo-list');
        
        if (!pendingList || !completedList) return;
        
        const todoItem = document.createElement('div');
        todoItem.className = 'todo-item';
        todoItem.setAttribute('data-todo-text', text);
        if (todoId) {
            todoItem.setAttribute('data-todo-id', todoId);
        }
        
        if (isCompleted) {
            todoItem.innerHTML = `
                <input type="checkbox" class="todo-checkbox" checked>
                <span class="todo-text completed-text">${text}</span>
            `;
            completedList.appendChild(todoItem);
            updateCompletedCount();
        } else {
            todoItem.innerHTML = `
                <input type="checkbox" class="todo-checkbox">
                <span class="todo-text">${text}</span>
            `;
            pendingList.appendChild(todoItem);
            
            const checkbox = todoItem.querySelector('.todo-checkbox');
            checkbox.addEventListener('change', function() {
                if (this.checked) {
                    const itemTodoId = todoItem.getAttribute('data-todo-id');
                    if (itemTodoId) {
                        // 서버에 완료 처리 요청
                        completeTodoOnServer(itemTodoId, true)
                            .then(() => {
                                moveTodoToCompleted(text, itemTodoId);
                                todoItem.remove();
                                // 할일 완료 후 캘린더 업데이트 및 오늘의 할일 목록 새로고침
                                if (currentCalendarYear && currentCalendarMonth) {
                                    loadCalendarData(currentCalendarYear, currentCalendarMonth);
                                }
                                loadTodayTodos();
                            })
                            .catch(error => {
                                console.error('할일 완료 처리 실패:', error);
                                this.checked = false; // 체크 해제
                            });
                    } else {
                        moveTodoToCompleted(text, itemTodoId);
                        todoItem.remove();
                    }
                }
            });
        }
    }
    
    // API 호출 유틸리티 함수
    function apiCall(url, options = {}) {
        console.log('apiCall 요청:', url, options);
        return fetch(url, {
            method: options.method || 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                ...options.headers
            },
            credentials: 'same-origin', // 세션 쿠키 전송
            body: options.body ? JSON.stringify(options.body) : undefined
        })
        .then(response => {
            console.log('apiCall 응답:', response.status, response.statusText, response.url);
            // 응답 타입 확인
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                console.error('JSON 응답 아님:', contentType);
                throw new Error('서버에서 JSON 응답을 받지 못했습니다.');
            }
            return response.json().then(data => {
                console.log('apiCall 응답 데이터:', data);
                // HTTP 에러 상태 코드 처리
                if (!response.ok) {
                    throw new Error(data?.error || options.errorMessage || `HTTP ${response.status}: 요청 실패`);
                }
                // 응답이 success: false인 경우 에러 처리
                if (data && typeof data === 'object' && data.success === false) {
                    throw new Error(data.error || options.errorMessage || '요청 실패');
                }
                return data;
            }).catch(err => {
                // JSON 파싱 에러 처리
                if (err instanceof SyntaxError) {
                    console.error('JSON 파싱 에러:', err);
                    throw new Error('서버 응답을 파싱할 수 없습니다: ' + err.message);
                }
                throw err;
            });
        })
        .then(data => {
            if (options.successMessage) {
                console.log(options.successMessage, data);
            }
            return options.dataPath ? data[options.dataPath] : data;
        })
        .catch(error => {
            console.error(options.errorMessage || '요청 실패', error);
            throw error;
        });
    }
    
    // 서버에 할일 저장
    function saveTodoToServer(title, targetDate, isCompleted = false) {
        console.log('할일 저장 요청:', { title, targetDate, isCompleted });
        return apiCall('/mypage/todos/save', {
            method: 'POST',
            body: { 
                title: title, 
                targetDate: targetDate, 
                isCompleted: isCompleted,
                description: ''
            },
            dataPath: 'todo',
            successMessage: '할일 저장 성공',
            errorMessage: '할일 저장 실패'
        });
    }
    
    // 서버에 할일 완료 처리
    function completeTodoOnServer(todoId, isCompleted) {
        return apiCall(`/mypage/todos/${todoId}/complete?completed=${isCompleted}`, {
            method: 'PUT',
            dataPath: 'todo',
            successMessage: '할일 완료 처리 성공',
            errorMessage: '할일 완료 처리 실패'
        });
    }

    function moveTodoToCompleted(text, todoId = null) {
        const completedList = document.getElementById('completed-todo-list');
        if (!completedList) return;
        
        // 현재 모달의 날짜 정보 가져오기
        const dateDisplay = document.getElementById('day-detail-date');
        let dateStr = '';
        if (dateDisplay) {
            const dateText = dateDisplay.textContent;
            const match = dateText.match(/(\d+)월 (\d+)일/);
            if (match) {
                const month = parseInt(match[1]);
                const day = parseInt(match[2]);
                const date = new Date(currentCalendarYear || new Date().getFullYear(), month - 1, day);
                const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
                dateStr = `${month}월 ${day}일 (${weekdays[date.getDay()]})`;
            }
        }
        
        if (!dateStr) {
            const now = new Date();
            dateStr = `${now.getMonth() + 1}월 ${now.getDate()}일 (${['일', '월', '화', '수', '목', '금', '토'][now.getDay()]})`;
        }
        
        const completedItem = document.createElement('div');
        completedItem.className = 'todo-item completed-todo-item';
        completedItem.setAttribute('data-todo-text', text);
        if (todoId) {
            completedItem.setAttribute('data-todo-id', todoId);
        }
        completedItem.innerHTML = `
            <input type="checkbox" class="todo-checkbox" checked>
            <div class="todo-content">
                <span class="todo-text completed-text">${text}</span>
                <div class="completed-date">완료일: ${dateStr}</div>
            </div>
        `;
        
        completedList.appendChild(completedItem);
        updateCompletedCount();
    }

    function updateCompletedCount() {
        const completedList = document.getElementById('completed-todo-list');
        const countElement = document.getElementById('completed-count');
        if (completedList && countElement) {
            const count = completedList.querySelectorAll('.todo-item').length;
            countElement.textContent = count;
        }
    }

    // 완료된 할일 섹션 접기/펼치기
    const completedHeader = document.getElementById('completed-header');
    const completedList = document.getElementById('completed-todo-list');
    
    if (completedHeader && completedList) {
        completedHeader.addEventListener('click', function() {
            const isHidden = completedList.style.display === 'none';
            completedList.style.display = isHidden ? 'block' : 'none';
            const toggleIcon = this.querySelector('.completed-toggle-icon');
            if (toggleIcon) {
                toggleIcon.textContent = isHidden ? '▼' : '▶';
            }
        });
    }

    // 할일 로드 (서버에서)
    function loadDayTodos(year, month, day) {
        const pendingList = document.getElementById('pending-todo-list');
        const completedList = document.getElementById('completed-todo-list');
        
        if (pendingList) pendingList.innerHTML = '';
        if (completedList) completedList.innerHTML = '';
        
        apiCall(`/mypage/todos/list?year=${year}&month=${month}&day=${day}`, {
            errorMessage: '할일 로드 실패'
        })
        .then(todos => {
            todos.forEach(todo => {
                addTodoItem(todo.title, todo.isCompleted, todo.todoId);
            });
        })
        .catch(() => {
            // 에러 발생 시 빈 목록 표시
        });
    }
    

    // 오늘 수강한 강의 목록 로드
    function loadDailyCourses(year, month, day) {
        const coursesList = document.getElementById('daily-courses-list');
        if (!coursesList) {
            console.error('daily-courses-list 요소를 찾을 수 없습니다.');
            return;
        }

        console.log('일일 강의 목록 로드 시작:', { year, month, day });

        // 로딩 메시지 표시
        coursesList.innerHTML = '<p class="loading-message">오늘 수강한 강의를 불러오는 중...</p>';

        const params = new URLSearchParams({
            year: String(year),
            month: String(month),
            day: String(day)
        });

        const apiUrl = `/api/mypage/daily-courses?${params.toString()}`;
        console.log('API 호출 URL:', apiUrl);

        apiCall(apiUrl)
            .then(data => {
                console.log('API 응답 데이터:', data);
                
                if (!data || data.success === false) {
                    console.error('일일 강의 목록 로드 실패:', data && data.error);
                    coursesList.innerHTML = '<p class="empty-courses">오늘 수강한 강의가 없습니다.</p>';
                    return;
                }

                const courses = data.courses || [];
                console.log('강의 목록:', courses);

                if (!courses.length) {
                    console.log('강의 목록이 비어있습니다.');
                    coursesList.innerHTML = '<p class="empty-courses">오늘 수강한 강의가 없습니다.</p>';
                    return;
                }

                coursesList.innerHTML = '';

                courses.forEach(course => {
                    const item = document.createElement('div');
                    item.className = 'daily-course-item';

                    const courseTitle = course.courseTitle || '제목 없는 강의';
                    const chapterTitle = course.chapterTitle;
                    const studiedMinutes = course.studiedMinutes ? Math.round(course.studiedMinutes) : null;

                    item.innerHTML = `
                        <div class="daily-course-info">
                            <span class="daily-course-dot"></span>
                            <span class="daily-course-title">${courseTitle}</span>
                            ${chapterTitle ? `<span class="daily-course-chapter">・ ${chapterTitle}</span>` : ''}
                            ${studiedMinutes ? `<span class="daily-course-time">${studiedMinutes}분 학습</span>` : ''}
                        </div>
                    `;

                    coursesList.appendChild(item);
                });
            })
            .catch(error => {
                console.error('일일 강의 목록 로드 실패:', error);
                coursesList.innerHTML = '<p class="empty-courses">오늘 수강한 강의가 없습니다.</p>';
            });
    }

    // 수료증 전체 보기 모달 관련
    const viewAllCertificatesBtn = document.getElementById('view-all-certificates-btn');
    const certificatesModal = document.getElementById('certificates-modal');
    const closeCertificatesModalBtn = document.getElementById('close-certificates-modal');

    // 수료증 전체 보기 버튼 클릭
    if (viewAllCertificatesBtn && certificatesModal) {
        viewAllCertificatesBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            certificatesModal.style.display = 'flex';
            loadAllCertificates();
        });
    }

    // 수료증 모달 닫기
    if (closeCertificatesModalBtn && certificatesModal) {
        closeCertificatesModalBtn.addEventListener('click', function() {
            certificatesModal.style.display = 'none';
        });
    }

    // 모달 외부 클릭 시 닫기
    if (certificatesModal) {
        certificatesModal.addEventListener('click', function(e) {
            if (e.target === certificatesModal) {
                certificatesModal.style.display = 'none';
            }
        });
    }

    // 수료증 목록 로드
    function loadAllCertificates() {
        const certificatesList = document.getElementById('certificates-list');
        if (!certificatesList) return;

        certificatesList.innerHTML = '<p class="loading-message">수료증 목록을 불러오는 중...</p>';

        fetch('/api/mypage/certificates', {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                throw new Error('서버에서 JSON 응답을 받지 못했습니다.');
            }
            return response.json();
        })
        .then(data => {
            if (data.success === false) {
                throw new Error(data.error || '수료증 목록을 불러올 수 없습니다.');
            }
            
            const certificates = data.certificates || data || [];
            
            if (certificates.length === 0) {
                certificatesList.innerHTML = '<p class="empty-message">수료증이 없습니다.</p>';
                return;
            }

            let html = '<div class="certificates-grid">';
            certificates.forEach(cert => {
                const issuedDate = cert.issuedDate ? new Date(cert.issuedDate).toLocaleDateString('ko-KR', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit'
                }).replace(/\./g, '.').replace(/\s/g, ' ') : '-';
                
                html += `
                    <div class="certificate-modal-item">
                        <div class="certificate-modal-icon">📜</div>
                        <div class="certificate-modal-info">
                            <div class="certificate-modal-title">${cert.courseTitle || '강의명 없음'}</div>
                            <div class="certificate-modal-date">${issuedDate}</div>
                        </div>
                        <a href="/mypage/certificates/${cert.certificateId}/download" 
                           class="certificate-download-btn" 
                           download>
                            다운로드
                        </a>
                    </div>
                `;
            });
            html += '</div>';
            certificatesList.innerHTML = html;
        })
        .catch(error => {
            console.error('수료증 목록 로드 실패:', error);
            certificatesList.innerHTML = '<p class="error-message">수료증 목록을 불러오는데 실패했습니다: ' + error.message + '</p>';
        });
    }

})();
