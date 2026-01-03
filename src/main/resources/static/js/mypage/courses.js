document.addEventListener('DOMContentLoaded', function() {
    const filterBadges = document.querySelectorAll('.filter-badge');
    const courseCards = document.querySelectorAll('.course-card-horizontal');
    const noResultsMsg = document.getElementById('no-filter-results');

    function filterCourses(filter) {
        let visibleCount = 0;
        
        courseCards.forEach(card => {
            const status = card.getAttribute('data-status');
            if (filter === 'all' || filter === status) {
                card.style.display = 'flex'; 
                visibleCount++;
            } else {
                card.style.display = 'none';
            }
        });
        
        // 결과 없음 메시지 표시 여부
        if (visibleCount === 0 && courseCards.length > 0) {
             if(noResultsMsg) noResultsMsg.style.display = 'block';
        } else {
             if(noResultsMsg) noResultsMsg.style.display = 'none';
        }
    }

    filterBadges.forEach(badge => {
        badge.addEventListener('click', function() {
            // Remove active class from all
            filterBadges.forEach(b => b.classList.remove('active'));
            // Add active to clicked
            this.classList.add('active');
            
            const filter = this.getAttribute('data-filter');
            filterCourses(filter);
        });
    });
});
