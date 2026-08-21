import http from 'k6/http';
import {check, sleep} from 'k6';
import {Rate, Trend} from 'k6/metrics';

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

// 쓰기
const signupTrend = new Trend('signup_duration');
const contentCreateTrend = new Trend('content_create_duration');
const playlistCreateTrend = new Trend('playlist_create_duration');

// Test Options
export const options = {
  scenarios: {

    // 조회 부하
    read_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        {duration: '1m', target: 50}, // 1분간 0VU → 50VU로 서서히 증가
        {duration: '1m', target: 100}, // 2분간 50VU → 100VU로 서서히 증가
        {duration: '2m', target: 200}, // 3분간 100VU → 200VU로 서서히 증가
        {duration: '3m', target: 500}, // 3분간 200VU → 500VU로 서서히 증가
        {duration: '3m', target: 200}, // 3분간 500VU → 200VU로 서서히 감소
        {duration: '2m', target: 100}, // 2분간 200VU → 100VU로 서서히 감소
        {duration: '1m', target: 50}, // 1분간 100VU → 50VU으로 서서히 증가
        {duration: '1m', target: 0}, // 1분간 50VU → 0VU으로 서서히 감소
      ],
      exec: 'readLoad',
    },

    // 5분 시점
    write_1: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      startTime: '5m',
      maxDuration: '30s',
      exec: 'write1',
    },

    // 8분 시점
    write_2: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      startTime: '8m',
      maxDuration: '30s',
      exec: 'write2',
    },

    // 10분 시점
    write_3: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      startTime: '10m',
      maxDuration: '30s',
      exec: 'write3',
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'], // P95는 200ms 미만, P99는 500ms 미만
    error_rate: ['rate<0.01'],
  },
};

// 환경 구성
const BASE_URL = 'http://localhost:8080/api';

const PASSWORD = '12345678';

const ADMIN_EMAIL = 'admin@mople.com';
const ADMIN_PASSWORD = 'Admin1234!';

// 시드 데이터의 실제 Content UUID
const SAMPLE_CONTENT_IDS = [
  'b98308d3-fe81-4676-947d-6b4f704d3118',
  'eee6c3b9-54b3-4ad7-bc8b-72aa38b8e944',
  'cdef410b-956b-44db-9a4e-5f92ffc22dba',
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

// 부하 테스트에서 수정할 기존 사용자
const LOAD_TEST_USER_EMAIL = 'test1@test.com';
const LOAD_TEST_USER_ORIGINAL_NAME = '사용자 1';
const LOAD_TEST_USER_MODIFIED_NAME = '부하테스트 사용자 수정';

// 부하 테스트에서 생성한 Content ID
let loadTestContentIds = [];

// 부하 테스트에서 생성한 Playlist ID
let loadTestPlaylistIds = [];

// 수정한 사용자 ID
let loadTestUserId = null;

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
export function readLoad(data) {

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
      `${BASE_URL}/contents?keywordLike=${encodeURIComponent(
          contentKeyword)}&limit=20&sortDirection=DESCENDING&sortBy=createdAt`,
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
      `${BASE_URL}/playlists?keywordLike=${encodeURIComponent(
          playlistKeyword)}&limit=20&sortDirection=DESCENDING&sortBy=updatedAt`,
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
      `${BASE_URL}/users?emailLike=${encodeURIComponent(
          userKeyword)}&limit=20&sortDirection=DESCENDING&sortBy=email`,
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

// 5분 시점 쓰기 작업
export function write1(data) {
  executeWriteScenario(data.adminAccessToken, 'write1', 0, 2, 12);
}

// 8분 시점 쓰기 작업
export function write2(data) {
  executeWriteScenario(data.adminAccessToken, 'write2', 0, 0, 8);

  // 2번째 시점에 1번째 시점에서 생성한 콘텐츠 1개 수정
  updateLoadTestContent(data.adminAccessToken);

  // 2번째 시점에 기존 사용자 1명 수정
  updateLoadTestUser(data.adminAccessToken);
}

// 10분 시점 쓰기 작업
export function write3(data) {

  // 3번째 시점에 생성한 콘텐츠 2개 삭제
  deleteLoadTestContents(data.adminAccessToken);

  // 3번째 시점에 생성한 플레이리스트 20개 삭제
  deleteLoadTestPlaylists(data.adminAccessToken);

  // 3번째 시점에 수정했던 사용자 1명 원복
  restoreLoadTestUser(data.adminAccessToken);
}

// 사용자, 콘텐츠, 플레이리스트 추가
function executeWriteScenario(adminAccessToken, prefix, signupCount,
    contentCount, playlistCount) {

  // 1. 사용자 회원가입
  for (let i = 1; i <= signupCount; i++) {

    const uniqueId = `${prefix}_${Date.now()}_${i}`;

    const signUpRes = http.post(
        `${BASE_URL}/users`,
        JSON.stringify({
          email: `loadtest_${uniqueId}@test.com`,
          password: PASSWORD,
          name: `부하테스트 사용자 ${uniqueId}`,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
    );

    const signUpSuccess = check(signUpRes, {
      'user signup status is 201': (r) => r.status === 201,
    });

    errorRate.add(!signUpSuccess);
    signupTrend.add(signUpRes.timings.duration);
  }

  // 2. 관리자 콘텐츠 추가
  for (let i = 1; i <= contentCount; i++) {

    const uniqueId = `${prefix}_${Date.now()}_${i}`;

    const contentRes = http.post(
        `${BASE_URL}/contents`,
        JSON.stringify({
          type: 'MOVIE',
          title: `부하테스트 콘텐츠 ${uniqueId}`,
          description: `부하테스트용 콘텐츠 ${uniqueId}`,
          thumbnailUrl: 'https://image.tmdb.org/t/p/w500/86EdVHOEFjFNPeE0sWjpCqcUORj.jpg',
          tags: ['테스트'],
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${adminAccessToken}`,
          },
        }
    );

    const contentCreateSuccess = check(contentRes, {
      'content create status is 201': (r) => r.status === 201,
    });

    errorRate.add(!contentCreateSuccess);
    contentCreateTrend.add(contentRes.timings.duration);

    if (contentCreateSuccess) {
      const content = contentRes.json();

      if (content.id) {
        loadTestContentIds.push(content.id);
      }
    }
  }

  // 3. 관리자 플레이리스트 추가
  for (let i = 1; i <= playlistCount; i++) {

    const uniqueId = `${prefix}_${Date.now()}_${i}`;

    const playlistRes = http.post(
        `${BASE_URL}/playlists`,
        JSON.stringify({
          title: `부하테스트 플레이리스트 ${uniqueId}`,
          description: `부하테스트용 플레이리스트 ${uniqueId}`,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${adminAccessToken}`,
          },
        }
    );

    const playlistCreateSuccess = check(playlistRes, {
      'playlist create status is 201': (r) => r.status === 201,
    });

    errorRate.add(!playlistCreateSuccess);
    playlistCreateTrend.add(playlistRes.timings.duration);

    if (playlistCreateSuccess) {
      const playlist = playlistRes.json();

      if (playlist.id) {
        loadTestPlaylistIds.push(playlist.id);
      }
    }
  }
}

// 1번째 쓰기 작업에서 생성된 콘텐츠 1개 수정
function updateLoadTestContent(adminAccessToken) {

  if (loadTestContentIds.length === 0) {
    return;
  }

  const contentId = loadTestContentIds[0];

  const contentDetailRes = http.get(
      `${BASE_URL}/contents/${contentId}`,
      {
        headers: {
          Authorization: `Bearer ${adminAccessToken}`,
        },
      }
  );

  const detailSuccess = check(contentDetailRes, {
    'load test content detail status is 200': (r) => r.status === 200,
  });

  errorRate.add(!detailSuccess);

  if (!detailSuccess) {
    return;
  }

  const content = contentDetailRes.json();

  const updateRes = http.patch(
      `${BASE_URL}/contents/${contentId}`,
      JSON.stringify({
        description: '부하테스트 수정 요청',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${adminAccessToken}`,
        },
      }
  );

  const updateSuccess = check(updateRes, {
    'content update status is 200': (r) => r.status === 200,
  });

  errorRate.add(!updateSuccess);
}

// 기존 사용자 1명 수정
function updateLoadTestUser(adminAccessToken) {

  const userSearchRes = http.get(
      `${BASE_URL}/users?emailLike=${encodeURIComponent(
          LOAD_TEST_USER_EMAIL)}&limit=20&sortDirection=DESCENDING&sortBy=email`,
      {
        headers: {
          Authorization: `Bearer ${adminAccessToken}`,
        },
      }
  );

  const searchSuccess = check(userSearchRes, {
    'load test user search status is 200': (r) => r.status === 200,
  });

  errorRate.add(!searchSuccess);

  if (!searchSuccess) {
    return;
  }

  const users = extractItems(userSearchRes);

  const user = users.find(
      (item) => item.email === LOAD_TEST_USER_EMAIL
  );

  if (!user) {
    console.log(
        `부하 테스트 사용자 조회 실패: email=${LOAD_TEST_USER_EMAIL}`
    );
    return;
  }

  loadTestUserId = user.id;

  const updateRes = http.patch(
      `${BASE_URL}/users/${loadTestUserId}`,
      {
        request: JSON.stringify({
          name: LOAD_TEST_USER_MODIFIED_NAME,
        }),
        image: http.file('', 'empty'),
      }
  );

  const updateSuccess = check(updateRes, {
    'load test user update status is 200': (r) => r.status === 200,
  });

  errorRate.add(!updateSuccess);
}

// 3번째 시점에 생성된 콘텐츠 2개 삭제
function deleteLoadTestContents(adminAccessToken) {

  for (const contentId of loadTestContentIds) {

    const deleteRes = http.del(
        `${BASE_URL}/contents/${contentId}`,
        null,
        {
          headers: {
            Authorization: `Bearer ${adminAccessToken}`,
          },
        }
    );

    const deleteSuccess = check(deleteRes, {
      'load test content delete status is 200': (r) => r.status === 200,
    });

    errorRate.add(!deleteSuccess);
  }

  loadTestContentIds = [];
}

// 3번째 시점에 생성된 플레이리스트 20개 삭제
function deleteLoadTestPlaylists(adminAccessToken) {

  for (const playlistId of loadTestPlaylistIds) {

    const deleteRes = http.del(
        `${BASE_URL}/playlists/${playlistId}`,
        null,
        {
          headers: {
            Authorization: `Bearer ${adminAccessToken}`,
          },
        }
    );

    const deleteSuccess = check(deleteRes, {
      'load test playlist delete status is 204': (r) => r.status === 204,
    });

    errorRate.add(!deleteSuccess);
  }

  loadTestPlaylistIds = [];
}

// 수정했던 사용자 원복
function restoreLoadTestUser(adminAccessToken) {

  if (!loadTestUserId) {
    return;
  }

  const updateRes = http.patch(
      `${BASE_URL}/users/${loadTestUserId}`,
      {
        request: JSON.stringify({
          name: LOAD_TEST_USER_ORIGINAL_NAME,
        }),
        image: http.file('', 'empty'),
      }
  );

  const restoreSuccess = check(updateRes, {
    'load test user restore status is 200': (r) => r.status === 200,
  });

  errorRate.add(!restoreSuccess);

  loadTestUserId = null;
}

// 목록 응답에서 실제 데이터 배열 추출
function extractItems(response) {

  const body = response.json();

  if (Array.isArray(body)) {
    return body;
  }

  if (Array.isArray(body.content)) {
    return body.content;
  }

  if (Array.isArray(body.data)) {
    return body.data;
  }

  if (Array.isArray(body.items)) {
    return body.items;
  }

  return [];
}