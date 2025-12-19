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
        
        // 날짜 칸 추가
        for (let day = 1; day <= daysInMonth; day++) {
            const dayData = dayMap.get(day);
            const hasStudy = dayData && dayData.hasStudy === true;
            const hasAttendance = dayData && dayData.hasAttendance === true;
            const lectureCount = dayData && dayData.lectureCount ? dayData.lectureCount : 0;
            const studyMinutes = dayData && dayData.studyMinutes ? dayData.studyMinutes : 0;
            
            let dayClass = 'calendar-day clickable-day';
            if (hasStudy) dayClass += ' has-study';
            if (hasAttendance) dayClass += ' has-attendance';
            
            let statsHtml = '';
            if (lectureCount > 0 || studyMinutes > 0) {
                statsHtml = '<div class="calendar-day-stats">';
                if (lectureCount > 0) statsHtml += lectureCount + '강';
                if (studyMinutes > 0) statsHtml += ' ' + studyMinutes + '분';
                statsHtml += '</div>';
            }
            
            html += '<div class="' + dayClass + '" data-day="' + day + '" data-year="' + year + '" data-month="' + month + '">';
            html += '<div class="calendar-day-number">' + day + '</div>';
            html += statsHtml;
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
    
    // 페이지 로드 완료 후 실행
    document.addEventListener('DOMContentLoaded', function() {
        try {
            // 전역 변수에서 캘린더 데이터 가져오기 (HTML에서 설정됨)
            const calendarData = window.dashboardCalendarData || null;
            const calendarEl = document.getElementById('dashboard-calendar');
            
            if (calendarData && calendarData.year && calendarData.month && calendarEl) {
                currentCalendarYear = calendarData.year;
                currentCalendarMonth = calendarData.month;
                updateCalendarMonthDisplay();
                renderCalendar(calendarData);
            } else if (calendarEl) {
                // 데이터가 없어도 기본 캘린더 표시
                const now = new Date();
                currentCalendarYear = now.getFullYear();
                currentCalendarMonth = now.getMonth() + 1;
                updateCalendarMonthDisplay();
                renderCalendar({
                    year: currentCalendarYear,
                    month: currentCalendarMonth,
                    days: []
                });
            }
        } catch (e) {
            console.error('캘린더 렌더링 오류:', e);
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
                    class: parseInt(classInput.value) || 2,
                    time: parseInt(timeInput.value) || 10,
                    note: parseInt(noteInput.value) || 2
                };
                
                // TODO: 서버에 저장하는 API 호출
                console.log('저장할 목표:', goals);
                alert('일일 학습 목표가 저장되었습니다.');
                dailyGoalModal.style.display = 'none';
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
        const day = d.getDay();
        const diff = d.getDate() - day + (day === 0 ? -6 : 1);
        return new Date(d.setDate(diff));
    }

    function updateWeeklyLearning(offset) {
        const now = new Date();
        const targetDate = new Date(now);
        targetDate.setDate(now.getDate() + (offset * 7));
        
        const startOfWeek = getWeekStart(targetDate);
        currentWeekStart = new Date(startOfWeek);
        
        const year = startOfWeek.getFullYear();
        const month = startOfWeek.getMonth() + 1;
        const weekNumber = Math.ceil(startOfWeek.getDate() / 7);
        
        const weekLabelEl = document.getElementById('week-label-display');
        if (weekLabelEl) {
            weekLabelEl.textContent = `${year}년 ${month}월 ${weekNumber}주차`;
        }
        
        updateWeeklyCalendar(startOfWeek);
    }

    function updateWeeklyCalendar(startOfWeek) {
        const dayNames = ['월', '화', '수', '목', '금', '토', '일'];
        const weeklyCalendar = document.querySelector('.weekly-calendar');
        if (!weeklyCalendar) return;
        
        let html = '';
        for (let i = 0; i < 7; i++) {
            const date = new Date(startOfWeek);
            date.setDate(startOfWeek.getDate() + i);
            const day = date.getDate();
            const dayOfWeek = dayNames[i];
            
            // TODO: 서버에서 해당 날짜의 학습 데이터 가져오기
            const hasStudy = i === 1;
            const lectureCount = hasStudy ? 2 : 0;
            const studyMinutes = hasStudy ? 45 : 0;
            const noteCount = hasStudy ? 1 : 0;
            
            html += `
                <div class="day-item">
                    <div class="day-name">${dayOfWeek}</div>
                    <div class="day-circle day-tooltip-trigger ${hasStudy ? 'has-study' : ''}" 
                         data-day="${day}"
                         data-day-of-week="${dayOfWeek}"
                         data-lecture-count="${lectureCount}"
                         data-study-minutes="${studyMinutes}"
                         data-note-count="${noteCount}">
                    </div>
                    <div class="day-tooltip">
                        <div class="tooltip-date">${startOfWeek.getFullYear()}. ${startOfWeek.getMonth() + 1}. ${day}. ${dayOfWeek}</div>
                        <div class="tooltip-stats">
                            <div class="tooltip-stat-item">
                                <span class="tooltip-icon purple">📝</span>
                                <span class="tooltip-label">노트</span>
                                <span class="tooltip-value">${noteCount}개</span>
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

    // 더미 캘린더 데이터 생성
    function generateDummyCalendarData(year, month) {
        const days = [];
        const firstDay = new Date(year, month - 1, 1);
        const daysInMonth = new Date(year, month, 0).getDate();
        
        for (let day = 1; day <= daysInMonth; day++) {
            // 랜덤하게 일부 날짜에만 학습 데이터
            if (day % 3 === 0 || day === 5 || day === 12 || day === 20) {
                days.push({
                    day: day,
                    lectureCount: Math.floor(Math.random() * 3) + 1,
                    studyMinutes: Math.floor(Math.random() * 60) + 30,
                    hasAttendance: true,
                    hasStudy: true
                });
            } else {
                days.push({
                    day: day,
                    lectureCount: 0,
                    studyMinutes: 0,
                    hasAttendance: false,
                    hasStudy: false
                });
            }
        }
        
        return {
            year: year,
            month: month,
            days: days
        };
    }

    // 서버에서 캘린더 데이터 가져오기 (또는 더미 데이터 사용)
    function loadCalendarData(year, month) {
        // TODO: 서버 API 호출
        // fetch(`/api/mypage/calendar?year=${year}&month=${month}`)
        //     .then(response => response.json())
        //     .then(data => {
        //         renderCalendar(data);
        //     })
        //     .catch(error => {
        //         console.error('캘린더 데이터 로드 실패:', error);
        //         renderCalendar(generateDummyCalendarData(year, month));
        //     });
        
        // 현재는 더미 데이터 사용
        const calendarData = generateDummyCalendarData(year, month);
        renderCalendar(calendarData);
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

    // 할일 추가
    if (newTodoTitle) {
        newTodoTitle.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                const text = this.value.trim();
                if (text) {
                    // 날짜 정보 가져오기
                    const dateDisplay = document.getElementById('day-detail-date');
                    let year = currentCalendarYear || new Date().getFullYear();
                    let month = new Date().getMonth() + 1;
                    let day = new Date().getDate();
                    
                    if (dateDisplay) {
                        const dateText = dateDisplay.textContent;
                        const match = dateText.match(/(\d+)월 (\d+)일/);
                        if (match) {
                            month = parseInt(match[1]);
                            day = parseInt(match[2]);
                            year = currentCalendarYear || new Date().getFullYear();
                        }
                    }
                    
                    const targetDate = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                    
                    // 서버에 저장
                    saveTodoToServer(text, targetDate, false)
                        .then(savedTodo => {
                            console.log('할일 저장 응답:', savedTodo);
                            if (savedTodo && savedTodo.todoId) {
                                addTodoItem(text, false, savedTodo.todoId);
                                this.value = '';
                                if (todoInputSection) {
                                    todoInputSection.style.display = 'none';
                                }
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
            }
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
                        completeTodoOnServer(itemTodoId, true);
                    }
                    moveTodoToCompleted(text, itemTodoId);
                    todoItem.remove();
                }
            });
        }
    }
    
    // API 호출 유틸리티 함수
    function apiCall(url, options = {}) {
        return fetch(url, {
            method: options.method || 'GET',
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            body: options.body ? JSON.stringify(options.body) : undefined
        })
        .then(response => {
            return response.json().then(data => {
                // 응답이 success: false인 경우 에러 처리
                if (data.success === false) {
                    throw new Error(data.error || options.errorMessage || '요청 실패');
                }
                if (!response.ok) {
                    throw new Error(data.error || options.errorMessage || '요청 실패');
                }
                return data;
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
            body: { title, targetDate, isCompleted },
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
        if (!coursesList) return;
        
        coursesList.innerHTML = '';
        
        // TODO: 서버 API 호출하여 강의 목록 가져오기
        // 현재는 빈 목록 표시
        coursesList.innerHTML = '<p class="empty-courses">오늘 수강한 강의가 없습니다.</p>';
    }

})();

