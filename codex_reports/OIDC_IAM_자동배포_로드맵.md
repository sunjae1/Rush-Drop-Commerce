# GitHub Actions + AWS OIDC/IAM 자동배포 로드맵

## 1. OIDC가 뭐냐

OIDC(OpenID Connect)는 GitHub Actions가 실행될 때마다 GitHub가 짧게 쓸 수 있는 신원 토큰을 발급하고,
AWS가 그 토큰을 검증해서 임시 자격 증명을 주는 방식이다.

핵심 차이:

- Access Key 방식: GitHub Secrets에 장기 자격 증명을 저장
- OIDC 방식: GitHub Secrets에 AWS 키를 저장하지 않음

즉, OIDC 방식은:

- AWS Access Key를 GitHub에 넣지 않아도 되고
- 매 실행마다 임시 자격 증명을 받아 쓰고
- 특정 리포지토리와 브랜치만 역할을 맡을 수 있게 IAM에서 제한할 수 있다

이번 프로젝트에서는 OIDC + IAM Role 방식이 가장 깔끔하다.

---

## 2. 이번 프로젝트 기준 결론

현재 확인된 값:

- GitHub 저장소: `sunjae1/MyShopping_FE`
- 배포 브랜치: `main`
- 빌드 폴더: `dist/`
- 빌드 명령어: `npm run build`

현재 프론트 환경변수 상태:

```dotenv
VITE_API_BASE_URL=
VITE_USE_DEMO_DATA=false
```

그리고 `.env.production` 주석 기준으로 현재 운영은
CloudFront behavior가 `/api` 를 백엔드로 넘기는 구조다.

그래서 이 프로젝트의 OIDC 자동배포 1차 버전에서는:

- AWS 관련 값만 GitHub Variables에 넣으면 되고
- AWS 관련 GitHub Secrets는 없어도 된다
- 프론트 `.env.production` 값을 GitHub에 옮길 필요도 거의 없다

---

## 3. 최종 목표 구조

흐름은 아래다.

1. `main` 브랜치에 push
2. GitHub Actions 실행
3. GitHub가 OIDC 토큰 발급
4. AWS IAM Role이 그 토큰을 검증
5. Actions가 임시 AWS 권한 획득
6. `dist/` 를 S3에 업로드
7. CloudFront invalidation 실행

---

## 4. 내가 AWS에서 해야 할 일

### 4-1. 먼저 준비할 값

AWS 콘솔 들어가기 전에 아래 4개를 메모한다.

- AWS Account ID
- AWS Region
- S3 Bucket 이름
- CloudFront Distribution ID

예시:

```text
AWS_ACCOUNT_ID=123456789012
AWS_REGION=ap-northeast-2
S3_BUCKET=my-shopping-fe-prod
CLOUDFRONT_DISTRIBUTION_ID=E123456789ABCDE
```

추가 확인:

- 지금 수동 배포가 버킷 루트에 올라가는지
- 아니면 `prod/` 같은 prefix 밑에 올라가는지

예:

- `s3://my-shopping-fe-prod/`
- `s3://my-shopping-fe-prod/prod/`

이건 꼭 지금 수동 배포 방식과 똑같이 맞춰야 한다.

---

### 4-2. IAM에 GitHub OIDC Provider 추가

AWS 콘솔 경로:

`IAM -> Access management -> Identity providers -> Add provider`

입력값:

- Provider type: `OpenID Connect`
- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

추가가 끝나면 AWS 계정 안에 GitHub OIDC provider가 생긴다.

주의:

- 이건 계정당 한 번만 만들면 된다.
- 이미 있으면 다시 만들 필요 없다.

---

### 4-3. 배포 권한 정책 만들기

AWS 콘솔 경로:

`IAM -> Policies -> Create policy`

정책 이름 예시:

- `GitHubActionsDeployMyShoppingFePolicy`

### 버킷 루트에 올릴 때 정책 예시

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ListBucket",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::my-shopping-fe-prod"
    },
    {
      "Sid": "ManageBucketObjects",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::my-shopping-fe-prod/*"
    },
    {
      "Sid": "InvalidateCloudFront",
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "arn:aws:cloudfront::123456789012:distribution/E123456789ABCDE"
    }
  ]
}
```

### 특정 prefix에만 올릴 때 정책 예시

예를 들어 `prod/` 아래만 배포한다면:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ListBucketWithPrefix",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::my-shopping-fe-prod",
      "Condition": {
        "StringLike": {
          "s3:prefix": [
            "prod/*",
            "prod"
          ]
        }
      }
    },
    {
      "Sid": "ManageObjectsInPrefix",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::my-shopping-fe-prod/prod/*"
    },
    {
      "Sid": "InvalidateCloudFront",
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "arn:aws:cloudfront::123456789012:distribution/E123456789ABCDE"
    }
  ]
}
```

정책 만들 때 반드시 실제 값으로 바꿀 것:

- `my-shopping-fe-prod`
- `123456789012`
- `E123456789ABCDE`
- `prod/*`

---

### 4-4. GitHub Actions 전용 IAM Role 만들기

AWS 콘솔 경로:

`IAM -> Roles -> Create role`

선택:

- Trusted entity type: `Web identity`
- Identity provider: `token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

AWS 콘솔에서 GitHub 항목을 바로 넣을 수 있으면 이렇게 입력하면 된다.

- GitHub organization: `sunjae1`
- GitHub repository: `MyShopping_FE`
- GitHub branch: `main`

주의:

- repository나 branch를 비워두면 wildcard로 넓어질 수 있다.
- 이번 프로젝트는 반드시 repository와 branch를 모두 구체적으로 넣는 쪽이 안전하다.

Role 이름 예시:

- `MyShoppingFeGitHubDeployRole`

권장:

- 역할 이름을 너무 일반적으로 짓지 말 것
- `GitHubActions` 같은 너무 뭉뚱그린 이름보다는 프로젝트/용도 포함

이 Role에는 방금 만든 정책
`GitHubActionsDeployMyShoppingFePolicy`
를 붙인다.

---

### 4-5. Role Trust Policy를 리포지토리 + 브랜치로 제한하기

Role 생성 후 아래로 들어간다.

`IAM -> Roles -> MyShoppingFeGitHubDeployRole -> Trust relationships -> Edit trust policy`

이번 프로젝트 1차 권장안은 `main` 브랜치 고정이다.

### 현재 저장소 기준 Trust Policy 예시

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:sunjae1/MyShopping_FE:ref:refs/heads/main"
        }
      }
    }
  ]
}
```

반드시 실제 AWS Account ID로 바꿀 것:

- `123456789012`

이 정책 의미:

- `sunjae1/MyShopping_FE` 저장소만
- `main` 브랜치에서 실행된 workflow만
- 이 Role을 Assume 할 수 있다

### 왜 이렇게 좁혀야 하냐

OIDC의 핵심 보안 포인트는 이 부분이다.

제한이 느슨하면:

- 다른 브랜치
- 다른 저장소
- 심하면 의도하지 않은 소스

에서도 Role을 맡을 수 있게 된다.

이번 프로젝트는 `main` 고정이 가장 단순하고 안전하다.

---

### 4-6. AWS 쪽 작업 완료 후 복사해둘 값

AWS 설정이 끝나면 아래 값만 GitHub에 가져오면 된다.

- `AWS_ROLE_ARN`

예시:

```text
arn:aws:iam::123456789012:role/MyShoppingFeGitHubDeployRole
```

---

## 5. 내가 GitHub에서 해야 할 일

AWS OIDC 방식 기준으로, AWS 관련 GitHub Secrets는 없어도 된다.

즉:

- `AWS_ACCESS_KEY_ID` 없음
- `AWS_SECRET_ACCESS_KEY` 없음

### GitHub Variables에 넣을 것

GitHub 경로:

`Repository -> Settings -> Secrets and variables -> Actions -> Variables`

등록 권장값:

```text
AWS_ROLE_ARN=arn:aws:iam::123456789012:role/MyShoppingFeGitHubDeployRole
AWS_REGION=ap-northeast-2
S3_BUCKET=my-shopping-fe-prod
CLOUDFRONT_DISTRIBUTION_ID=E123456789ABCDE
```

선택:

```text
S3_PREFIX=
```

`S3_PREFIX` 는 버킷 하위 경로에 배포할 때만 쓴다.

예:

```text
S3_PREFIX=prod
```

### GitHub Secrets에 넣을 것

AWS OIDC 기준:

- 없음

정말 AWS 관련 Secret은 안 넣어도 된다.

참고:

- `AWS_ROLE_ARN` 은 비밀값은 아니어서 Variable로 두는 걸 권장한다.
- 나중에 다른 앱 비밀값이 생기면 그건 별도 Secret으로 넣으면 된다.

---

## 6. 내가 workflow 파일에서 해야 할 일

만들 파일:

- `.github/workflows/deploy-fe.yml`

핵심 포인트는 3개다.

1. `permissions.id-token: write` 가 있어야 함
2. `aws-actions/configure-aws-credentials` 로 Role Assume 해야 함
3. 빌드 후 S3 업로드 + CloudFront invalidation 해야 함

### 버킷 루트 배포용 예시

```yaml
name: Deploy FE

on:
  push:
    branches:
      - main
  workflow_dispatch:

permissions:
  contents: read
  id-token: write

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm

      - name: Install dependencies
        run: npm ci

      - name: Build
        run: npm run build

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v6.0.0
        with:
          role-to-assume: ${{ vars.AWS_ROLE_ARN }}
          aws-region: ${{ vars.AWS_REGION }}

      - name: Upload to S3
        run: aws s3 sync dist s3://${{ vars.S3_BUCKET }} --delete

      - name: Invalidate CloudFront
        run: aws cloudfront create-invalidation --distribution-id ${{ vars.CLOUDFRONT_DISTRIBUTION_ID }} --paths "/*"
```

### prefix 배포용 예시

`prod/` 아래에 올리는 구조면:

```yaml
      - name: Upload to S3
        run: aws s3 sync dist s3://${{ vars.S3_BUCKET }}/${{ vars.S3_PREFIX }} --delete
```

---

## 7. 이 프로젝트에서 프론트 env는 어떻게 할까

현재 저장소 기준으로는:

```dotenv
VITE_API_BASE_URL=
VITE_USE_DEMO_DATA=false
```

그리고 코드 기본값도 운영과 맞아서,
지금 자동배포 1차에서는 GitHub Variables에 프론트 env를 안 넣어도 된다.

즉 이번 단계에서 필요한 GitHub 값은 사실상 이것뿐이다.

```text
AWS_ROLE_ARN
AWS_REGION
S3_BUCKET
CLOUDFRONT_DISTRIBUTION_ID
```

선택:

```text
S3_PREFIX
```

나중에 프론트 빌드 타임 환경변수가 실제로 추가되면 그때
`VITE_...` 값을 GitHub Variables 또는 Secrets로 넣으면 된다.

---

## 8. 첫 배포 테스트 순서

1. AWS에서 OIDC Provider 생성
2. IAM Policy 생성
3. IAM Role 생성
4. Trust Policy를 `repo:sunjae1/MyShopping_FE:ref:refs/heads/main` 으로 제한
5. Role ARN 복사
6. GitHub Variables 등록
7. `.github/workflows/deploy-fe.yml` 추가
8. `main` 에 push
9. GitHub Actions 로그 확인
10. CloudFront URL에서 결과 확인

로그에서 꼭 볼 것:

- `Configure AWS credentials` 성공 여부
- `aws sts get-caller-identity` 가 필요하면 디버그용으로 추가 가능
- `aws s3 sync` 성공 여부
- `create-invalidation` 성공 여부

---

## 9. 실패할 때 가장 흔한 원인

### 1. trust policy의 `sub` 값이 틀린 경우

예:

- repo 이름 오타
- owner 이름 오타
- branch 이름 오타

이번 프로젝트 기준 정확한 값은:

```text
repo:sunjae1/MyShopping_FE:ref:refs/heads/main
```

### 2. workflow에 `id-token: write` 가 없는 경우

이게 없으면 GitHub가 OIDC 토큰을 못 준다.

### 3. IAM 정책 리소스가 잘못된 경우

예:

- S3 버킷 ARN 오타
- CloudFront Distribution ID 오타
- prefix 경로 불일치

### 4. 수동 배포 경로와 자동 배포 경로가 다른 경우

예:

- 수동은 `bucket/prod/`
- 자동은 `bucket/`

이러면 배포가 된 것 같아도 실제 서비스가 이상해질 수 있다.

---

## 10. 권장 1차안과 2차안

### 1차 권장안

가장 먼저는 이걸 추천한다.

- `main` 브랜치 push 시 배포
- trust policy는 `main` 브랜치만 허용
- GitHub AWS Secret 없음
- GitHub Variables만 사용

이게 가장 단순하고 안정적이다.

### 2차 강화안

나중에 더 강화하고 싶으면:

- GitHub `environment: production` 사용
- environment protection rule 설정
- trust policy의 `sub` 를 environment 기준으로 변경

예시:

```text
repo:sunjae1/MyShopping_FE:environment:production
```

이 경우 workflow job에도 아래가 필요하다.

```yaml
environment: production
```

다만 지금은 1차로 `main` 브랜치 기준이 더 쉽다.

---

## 11. 이번 프로젝트 기준 최종 정리

### GitHub Variables

필수:

```text
AWS_ROLE_ARN
AWS_REGION
S3_BUCKET
CLOUDFRONT_DISTRIBUTION_ID
```

선택:

```text
S3_PREFIX
```

### GitHub Secrets

AWS OIDC 기준:

```text
없음
```

### AWS에서 만들 것

- GitHub OIDC Identity Provider
- S3 + CloudFront 배포 권한 IAM Policy
- GitHub Actions 전용 IAM Role

### Trust Policy 핵심 값

```text
repo:sunjae1/MyShopping_FE:ref:refs/heads/main
```

---

## 12. 내가 실제로 따라할 순서 한 줄 요약

1. AWS에서 OIDC Provider 만든다
2. S3/CloudFront 권한 정책 만든다
3. GitHub Actions 전용 IAM Role 만든다
4. Trust Policy를 `sunjae1/MyShopping_FE` + `main` 으로 잠근다
5. Role ARN을 GitHub Variables에 넣는다
6. workflow에서 `id-token: write` + `configure-aws-credentials@v6.0.0` 을 쓴다
7. S3 sync 후 CloudFront invalidation 한다

---

## 13. 공식 문서

- GitHub Docs: https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws
- AWS IAM User Guide: https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_create_for-idp_oidc.html
- AWS OIDC Provider Guide: https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html
- AWS Configure Credentials Action: https://github.com/marketplace/actions/configure-aws-credentials-action-for-github-actions
