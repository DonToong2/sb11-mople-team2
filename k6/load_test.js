import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';


// 커스텀 Metrics 추가

// 에러율
const errorRate = new Rate('error_rate');

// 로그인
const loginTrend = new Trend('login_duration');

// 조회
const contentDetailTrend = new Trend('content_detail_duration');
const contentListTrend = new Trend('content_list_duration');

// 검색
const contentSearchTrend = new Trend('content_search_duration');
const playlistSearchTrend = new Trend('playlist_search_duration');
const userSearchTrend = new Trend('user_search_duration');


// Test Options
export const options = {
  stages: [
    { duration: '30s', target: 50 },   // 30초간 0 -> 50VU로 서서히 증가
    { duration: '1m', target: 200 },   // 1분간 200VU까지 증가 후 유지
    { duration: '30s', target: 50 },   // 30초간 50VU까지 감소 후 유지
    { duration: '10s', target: 0 },    // 10초간 50 -> 0VU로 감소(종료)
  ],

  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'], // P95는 200ms 미만, P99는 500ms 미만
    error_rate: ['rate<0.01'],
  },
};


// 환경 구성
const BASE_URL = 'http://localhost:8080/api';

const PASSWORD = '12345678';

const ADMIN_EMAIL = 'admin@test.com';
const ADMIN_PASSWORD = 'admin1234';


// 시드 데이터의 실제 Content UUID
const SAMPLE_CONTENT_IDS = [
    // 테스트 콘텐츠 데이터 생성 후 실제 UUID 입력
  'CONTENT_UUID_1',
  'CONTENT_UUID_2',
  'CONTENT_UUID_3',
];


// 검색 키워드
const CONTENT_KEYWORDS = [
  '1',
  '10',
  '100',
  '1000',
  '9999',
];

const PLAYLIST_KEYWORDS = [
  '1',
  '10',
  '100',
  '1000',
  '10000',
  '19999',
];

const USER_KEYWORDS = [
  '1',
  '10',
  '100',
  '1000',
  '9999',
];


// Utility

function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function randomUser() {
  const userNumber = Math.floor(Math.random() * 10000) + 1;

  return {
    email: `test${userNumber}@test.com`,
    password: PASSWORD,
  };
}


// ADMIN 인증
export function setup() {
  const adminLoginRes = http.post(
      `${BASE_URL}/auth/sign-in`,
      {
        username: ADMIN_EMAIL,
        password: ADMIN_PASSWORD,
      }
  );

  const adminLoginSuccess = check(adminLoginRes, {
    'admin login status is 200': (r) => r.status === 200,
  });

  if (!adminLoginSuccess) {
    throw new Error(
        `Admin login failed: status=${adminLoginRes.status}, body=${adminLoginRes.body}`
    );
  }

  const adminAccessToken = adminLoginRes.json('accessToken');

  return {
    adminAccessToken,
  };
}


// 테스트 시나리오
export default function (data) {

  // 1. 사용자 로그인
  const user = randomUser();

  const loginRes = http.post(
      `${BASE_URL}/auth/sign-in`,
      {
        username: user.email,
        password: user.password,
      }
  );

  const loginSuccess = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
  });

  errorRate.add(!loginSuccess);
  loginTrend.add(loginRes.timings.duration);

  if (!loginSuccess) {
    return;
  }

  const accessToken = loginRes.json('accessToken');

  const authHeaders = {
    Authorization: `Bearer ${accessToken}`,
  };

  sleep(0.1);


  // 2. 콘텐츠 상세 조회
  const contentId = randomItem(SAMPLE_CONTENT_IDS);

  const detailRes = http.get(
      `${BASE_URL}/contents/${contentId}`,
      {
        headers: authHeaders,
      }
  );

  const detailSuccess = check(detailRes, {
    'content detail status is 200': (r) => r.status === 200,
  });

  errorRate.add(!detailSuccess);
  contentDetailTrend.add(detailRes.timings.duration);

  sleep(0.05);


  // 3. 콘텐츠 목록 조회
  const contentListRes = http.get(
      `${BASE_URL}/contents?limit=20&sortDirection=DESCENDING&sortBy=createdAt`,
      {
        headers: authHeaders,
      }
  );

  const contentListSuccess = check(contentListRes, {
    'content list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!contentListSuccess);
  contentListTrend.add(contentListRes.timings.duration);

  sleep(0.1);


  // 4. 콘텐츠 키워드 검색
  const contentKeyword = randomItem(CONTENT_KEYWORDS);

  const contentSearchRes = http.get(
      `${BASE_URL}/contents?keywordLike=${encodeURIComponent(contentKeyword)}&limit=20&sortDirection=DESCENDING&sortBy=createdAt`,
      {
        headers: authHeaders,
      }
  );

  const contentSearchSuccess = check(contentSearchRes, {
    'content search status is 200': (r) => r.status === 200,
  });

  errorRate.add(!contentSearchSuccess);
  contentSearchTrend.add(contentSearchRes.timings.duration);

  sleep(0.1);


  // 5. 플레이리스트 키워드 검색
  const playlistKeyword = randomItem(PLAYLIST_KEYWORDS);

  const playlistSearchRes = http.get(
      `${BASE_URL}/playlists?keywordLike=${encodeURIComponent(playlistKeyword)}&limit=20&sortDirection=DESCENDING&sortBy=updatedAt`,
      {
        headers: authHeaders,
      }
  );

  const playlistSearchSuccess = check(playlistSearchRes, {
    'playlist search status is 200': (r) => r.status === 200,
  });

  errorRate.add(!playlistSearchSuccess);
  playlistSearchTrend.add(playlistSearchRes.timings.duration);

  sleep(0.1);


  // 6. 사용자 키워드 검색
  const userKeyword = randomItem(USER_KEYWORDS);

  const adminAuthHeaders = {
    Authorization: `Bearer ${data.adminAccessToken}`,
  };

  const userSearchRes = http.get(
      `${BASE_URL}/users?emailLike=${encodeURIComponent(userKeyword)}&limit=20&sortDirection=DESCENDING&sortBy=email`,
      {
        headers: adminAuthHeaders,
      }
  );

  const userSearchSuccess = check(userSearchRes, {
    'user search status is 200': (r) => r.status === 200,
  });

  errorRate.add(!userSearchSuccess);
  userSearchTrend.add(userSearchRes.timings.duration);

  sleep(0.2);
}