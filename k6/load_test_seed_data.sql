-- Mople local 부하 테스트 시드 데이터
-- 시드 데이터 크기:
--   users    : 10,000
--   contents : 10,000
--   reviews  : 50,000
--   playlists: 20,000

-- 트랜잭션 시작
BEGIN;

-- 1. USERS
INSERT INTO users (
    id, created_at, email, password, name, profile_image_url,
    role, locked, temporary_password, temporary_password_expires_at,
    provider, provider_id
)
SELECT
    gen_random_uuid(),
    NOW() - (random() * INTERVAL '180 days'), -- 생성일을 현재~6개월 전까지로 설정
    'test' || gs || '@test.com', -- test1@test.com ~ test10000@test.com
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- 실제 암호화된 값이 들어가도록 설정
    'test' || gs, -- test1 ~ test10000
    NULL,
    'USER',
    false,
    NULL,
    NULL,
    'LOCAL', -- oauth 로그인을 제외 (BCryptPasswordEncoder 연산 속도를 파악하기 위해서)
    NULL
FROM generate_series(1, 10000) AS gs;

-- 2. CONTENTS
INSERT INTO contents (
    id, created_at, updated_at, type, title, description,
    "thumbnailUrl", rating_sum, review_count, watcher_count, external_id
)
SELECT
    gen_random_uuid(),
    created_at, -- 현재 ~ 180일 전 사이의 랜덤 생성일
    created_at + (random() * (NOW() - created_at)), -- created_at 이후 ~ 현재 사이의 랜덤 수정일
    CASE gs % 3 -- MOVIE, TV_SERIES, SPORT 순서대로 순환하여 생성(콘텐츠 타입)
    WHEN 0 THEN 'MOVIE'
        WHEN 1 THEN 'TV_SERIES'
        ELSE 'SPORT'
END,
    CASE gs % 3 -- 콘텐츠 제목(테스트 Movie 1, 테스트 TVSeries 2, 테스트 Sport 3, 테스트 Movie 4, ..., 10000까지)
        WHEN 0 THEN '테스트 Movie ' || gs
        WHEN 1 THEN '테스트 TVSeries ' || gs
        ELSE '테스트 Sport ' || gs
END,
    '콘텐츠 설명 ' || gs, -- 콘텐츠 설명 1 ~ 콘텐츠 설명 10000
    CASE -- TheSportsDB / TMDB 썸네일 URL을 번갈아 사용
        WHEN gs % 2 = 1
            THEN 'https://r2.thesportsdb.com/images/media/event/thumb/74tg1i1678700166.jpg'
        ELSE 'https://image.tmdb.org/t/p/w500/86EdVHOEFjFNPeE0sWjpCqcUORj.jpg'
END,
    0, -- ratingSum
    0, -- reviewCount
    (random() * 1000)::bigint, -- 시청자 수: 0 ~ 1,000명
   (1000000 + floor(random() * 9000000))::bigint -- 1,000,000 ~ 9,999,999 범위의 7자리 숫자
FROM (
    SELECT
        gs,
        NOW() - (random() * INTERVAL '180 days') AS created_at -- 현재 ~ 180일 전 사이의 랜덤 생성일
    FROM generate_series(1, 10000) AS gs
) AS data;

-- 3. CONTENT TAGS
INSERT INTO content_tags (content_id, tags)
SELECT
    id,
    CASE ROW_NUMBER() OVER (ORDER BY id) % 6 -- 태그를 액션~판타지 순으로 순환 생성
        WHEN 0 THEN '액션'
        WHEN 1 THEN '드라마'
        WHEN 2 THEN '코미디'
        WHEN 3 THEN '스릴러'
        WHEN 4 THEN '스포츠'
        ELSE '판타지'
    END
FROM contents;

-- 4. PLAYLISTS
INSERT INTO playlists (
    id, created_at, updated_at, owner_id,
    title, description, subscriber_count
)
SELECT
    gen_random_uuid(),
    created_at, -- 현재 ~ 180일 전 사이의 랜덤 생성일
    created_at + (random() * (NOW() - created_at)), -- created_at 이후 ~ 현재 사이의 랜덤 수정일
    CASE -- 사용자 7000명은 플레이리스트를 만들지 않음
        WHEN gs <= 4000 THEN (SELECT id FROM users ORDER BY id OFFSET floor(random() * 2000) LIMIT 1) -- 사용자 2,000명이 총 4000개의 플레이리스트를 분산 생성하도록 설정
        WHEN gs <= 9000 THEN (SELECT id FROM users ORDER BY id OFFSET 2000 + floor(random() * 700) LIMIT 1) -- 사용자 700명은 총 5000개의 플레이리스트를 분산 생성하도록 설정
    WHEN gs <= 14000 THEN (SELECT id FROM users ORDER BY id OFFSET 2700 + floor(random() * 250) LIMIT 1) -- 사용자 250명은 총 5000개의의 플레이리스트를 분산 생성하도록 설정
    ELSE (SELECT id FROM users ORDER BY id OFFSET 2950 + floor(random() * 50) LIMIT 1) -- 사용자 50명은 6000개의 플레이리스트를 생성하도록 설정
END,
    '테스트 플레이리스트 ' || gs,
    '테스트 플레이리스트 설명 ' || gs,
    (random() * 500)::bigint
FROM (
    SELECT
    gs,
    NOW() - (random() * INTERVAL '180 days') AS created_at
    FROM generate_series(1, 20000) AS gs
    ) AS data;

-- 5. REVIEWS
WITH numbered_contents AS ( -- (content_id, author_id) 복합 UNIQUE 제약 만족하도록 설정(콘텐츠 순번 부여)
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM contents
),
numbered_users AS ( -- (content_id, author_id) 복합 UNIQUE 제약 만족하도록 설정(사용자 순번 부여)
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM users
)
INSERT INTO reviews (
    id, created_at, content_id, author_id, text, rating
)
SELECT
    gen_random_uuid(),
    NOW() - (random() * INTERVAL '180 days'),
    c.id,
    u.id,
    '리뷰 ' || gs, -- 리뷰 내용(리뷰 1, ..., 리뷰 50000)
    (1 + floor(random() * 5))::double precision
FROM generate_series(1, 50000) AS gs
    JOIN numbered_users u
ON u.rn = ((gs - 1) / 5) + 1
    JOIN numbered_contents c -- 콘텐츠 3천개에는 리뷰가 달리지 않음
    ON c.rn = CASE
    WHEN (gs - 1) % 5 < 3 -- 사용자 리뷰 3개를 일반 콘텐츠 6,000개에 리뷰 3만개 분산
    THEN 1 + (((u.rn - 1) * 3 + ((gs - 1) % 5)) % 6000)
    WHEN (gs - 1) % 5 = 3 -- 사용자 리뷰 1개를 인기가 적당한 콘텐츠 900개에 리뷰 1만개 분산
    THEN 6001 + ((u.rn - 1) % 900)
    ELSE -- 사용자 리뷰 1개를 인기있는 콘텐츠 100개에 리뷰 1만개 분산
    6901 + ((u.rn - 1) % 100)
END;

-- 6. CONTENT REVIEW AGGREGATE
UPDATE contents c
SET
    rating_sum = r.rating_sum,
    review_count = r.review_count
FROM (
    SELECT
        content_id,
        SUM(rating) AS rating_sum,
        COUNT(*) AS review_count
    FROM reviews
    GROUP BY content_id
) r
WHERE c.id = r.content_id;

-- 트랜잭션 커밋(하나라도 실패하면 전부 롤백 되도록)
COMMIT;

-- 테이블과 테이블 데이터 개수가 원하는대로 잘 들어갔는지 확인
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'contents', COUNT(*) FROM contents
UNION ALL
SELECT 'content_tags', COUNT(*) FROM content_tags
UNION ALL
SELECT 'playlists', COUNT(*) FROM playlists
UNION ALL
SELECT 'reviews', COUNT(*) FROM reviews;
