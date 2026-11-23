# 学生端规格 / Student-side spec

**来源**：`Project Backlog table.docx`（以 backlog 为准）+ `User Stories5.docx` / `User Story Group 15.docx` 里 US01–US04 的验收标准。
**用途**：前端（React）先用 mock 数据实现；后端按此反推 API 和 SQLite 表。
标 **[PROPOSED]** 的是我们自己提议的命名和路径，不是 backlog 原文。标 **[OPEN]** 的需要团队决定。

主要用户：Yiming Chen，大一学生，**主要用手机**。需要：清楚的报名结果、最新的活动信息、提醒。

---

## 1. backlog 对照

| ID | 内容 | P | Size | 谁做 |
|---|---|---|---|---|
| B1 | 不登录也能浏览已发布的公开活动 | 1 | M | 前端 + 后端 |
| B2 | 搜索公开活动，无结果时给提示 | 2 | S | 前端 + 后端 |
| B3 | 查看活动介绍、时间、地点、要求、名额、截止时间 | 1 | M | 前端 + 后端 |
| B4 | 学生/组织者/老师登录 | 1 | M | 共用，前端 + 后端 |
| B7 | 仅俱乐部成员可见/可报 club-only 活动 | 1 | M | 后端判断，前端展示提示 |
| B15 | 详情页有 Register 按钮，和二维码指向同一个活动 | 1 | M | 前端 + 后端 |
| B16 | 确认报名前必须登录 | 1 | S | 前端跳转 + 后端校验 |
| B17 | 检查名额；**开始前 12 小时关闭报名** | 1 | M | 后端判断，前端展示原因 |
| B18 | 禁止重复报名；重复时显示已有报名 | 1 | M | 前端 + 后端 |
| B19 | "Registered Activities" 显示已确认报名和最新详情 | 1 | M | 前端 + 后端 |
| B21 | 学生可在**开始前 ≥24 小时**取消报名 | 1 | M | 前端 + 后端 |
| B22 | 取消后释放名额 | 1 | S | 后端 |
| B25 | 向学生清楚显示已取消的活动 | 2 | S | 前端 |
| B30 | 时间变更获批后，名额先"预留"，等学生确认 | 1 | L | 后端 + 前端 |
| B31 | 未确认的预留不惩罚学生 | 1 | M | 后端 |
| B32 | 新开始时间前 24 小时，未确认的预留自动取消并释放名额 | 1 | L | 后端（定时任务） |
| B33 | 复用取消流程来释放过期预留 | 1 | M | 后端 |
| B35 | 学生可确认或拒绝预留名额 | 1 | M | 前端 + 后端 |
| B36 | 学生为已报名活动设置邮件提醒 | 2 | L | 前端 + 后端 |
| B37 | 到提醒时间发送提醒邮件 | 2 | L | 后端（可模拟） |
| B24 / B29 | 取消、变更后给学生发邮件 | 1 | M | 后端；前端以站内通知呈现（邮件可模拟） |

> B22、B28、B31–B33、B37 是**系统行为**，学生界面只负责把结果显示出来。

---

## 2. 页面与路由 [PROPOSED]

| 路由 | 页面 | 作用 | backlog |
|---|---|---|---|
| `/login` | Login | 模拟统一认证登录；登录后回到原来想去的页面 | B4, B16 |
| `/activities` | ActivityList | 浏览 + 关键词搜索；无结果提示 | B1, B2 |
| `/activities/:id` | ActivityDetail | 详情 + Register 按钮 | B3, B15 |
| `/register/:token` | RegisterConfirm | 二维码落地页：显示活动 + "确认报名" | B15–B18 |
| `/my/registrations` | MyRegistrations | "Registered Activities"：我的报名 | B19, B25 |
| `/my/registrations/:id` | RegistrationDetail | 报名详情、取消、提醒、确认预留 | B21, B35, B36 |

学生登录后默认进入 `/activities`。导航：Activities、Registered Activities、退出。

---

## 3. 各页面内容与交互

### 3.1 ActivityList（B1, B2）
- 只显示**已发布 (published)** 的活动，未审核通过的绝不出现。
- 每项显示：标题、日期时间、地点、剩余名额、报名截止时间、状态标记（见 §4）。
- 搜索框按**标题关键词**搜索（Group 15 的 US01 还有按日期筛选，列为可选 **[OPEN]**）。
- **无结果**：显示"没有找到匹配的活动"，保留搜索词，提供"清空搜索"回到完整列表。
- 手机上不允许横向滚动。
- 要有：加载中、请求失败（带重试）、空列表三种状态。

### 3.2 ActivityDetail（B3, B15）
- 显示：介绍、起始日期时间、地点、参加要求、剩余名额 / 总名额、报名截止时间。
- **Register 按钮**的状态由 `registrationState` 决定：

| 情况 | 按钮 / 提示 |
|---|---|
| 可报名 | "Register" |
| 我已报名 | "你已报名"+"查看我的报名"，不再出现 Register |
| 已满 | 禁用，文字"名额已满" |
| 截止或开始前 12 小时内 | 禁用，文字"报名已截止" |
| 活动已取消 | 禁用，醒目显示"活动已取消"+原因 |
| club-only 且我不是成员 | 禁用，文字"仅限俱乐部成员" |

- 状态**不能只靠颜色**表达，必须有文字。

### 3.3 RegisterConfirm（B15–B18）
- 扫码进入。未登录先跳 `/login`，登录后回到这里（B16）。
- 显示活动信息 + "确认报名"按钮。**打开页面不占名额**，点击确认时服务端才重新检查。
- 成功：明确显示"你已成功报名"，给出**报名编号**，链接到"查看我的报名"。
- 失败要区分原因：`FULL` 名额已满、`CLOSED` 报名已截止、`CANCELLED` 活动已取消、`NOT_MEMBER` 仅限成员。
- **重复报名**（B18）：再次扫码或重复提交，显示"你已报名"和已有报名，不创建新记录，不报错。
- 二维码旁/页面上要有文字链接作为无障碍替代（"Registration link"）。

### 3.4 MyRegistrations（B19, B25）
- 只显示**当前学生自己的**报名，按活动开始时间升序（即将开始的在前）。
- 每项显示：活动名、时间、**当前**地点（从活动记录读取，不是报名时的旧值）、报名状态（见 §4）。
- 地点或时间被批准修改后，这里显示新值，并有"变更通知"。
- 活动取消：保留记录，标"活动已取消"+原因，不再算有效报名，不再有提醒。
- 空状态：提示"你还没有报名任何活动"+链接到 Activities。

### 3.5 RegistrationDetail（B21, B35, B36）
- **取消报名**：开始前 ≥24 小时才显示可用；点击后先确认（说明会释放名额）。距开始不足 24 小时时按钮禁用并说明"距开始不足 24 小时，无法取消"。
- **预留名额**（B30、B35）：状态为 `reserved` 时显示"时间已变更，请确认是否参加"，两个按钮：确认 / 拒绝，并显示确认截止时间（新开始时间前 24 小时）。
- **提醒**（B36，P2）：设置/修改/关闭提醒时间。时间必须**晚于现在、早于活动开始**；无效时说明原因并保留原设置。关闭提醒**不取消报名**。

---

## 4. 状态模型

**活动 (activity.status)**：`published`（学生只能看到这个，以及已报名的 `cancelled`）、`cancelled`。其余（draft、awaiting_review、returned）学生看不到。

**报名状态 (registrationState，由服务端算出，不存库)**，用于列表和详情的按钮：
`open` / `full` / `closed` / `cancelled`
- `closed` = 当前时间 > 报名截止时间，**或** 当前时间 > 开始时间 − 12 小时（B17）
- `full` = 有效报名数 ≥ capacity
- `remaining` = capacity − 有效报名数

**我的报名 (registration.status)**：`confirmed` | `cancelled` | `reserved`（时间变更后等待确认）

> "已满"和"我已报名"是两个独立维度：一个活动可以同时"已满"且"我已报名"。前者决定能否接受新报名，后者决定我能否查看自己的报名。

---

## 5. 学生看到的数据 [PROPOSED]

```ts
type User = { userId: string; displayName: string; role: 'student'|'organiser'|'teacher'; accountStatus: 'active'|'suspended' }

type Activity = {
  activityId: string; clubId: string; clubName: string
  title: string; description: string; location: string; requirements: string
  startsAt: string            // ISO 8601
  registrationDeadline: string
  capacity: number            // 正整数
  visibility: 'public'|'club_only'
  status: 'published'|'cancelled'
  cancelReason?: string
  registeredCount: number     // 派生
  remaining: number           // 派生
  registrationState: 'open'|'full'|'closed'|'cancelled'  // 派生
  myRegistration?: { registrationId: string; status: 'confirmed'|'reserved'|'cancelled' }  // 我已报名时才有
}

type Registration = {
  registrationId: string      // 可作报名编号
  studentId: string; activityId: string
  status: 'confirmed'|'cancelled'|'reserved'
  registeredAt: string
  reminderAt: string | null
  reservationDeadline?: string   // status=reserved 时
  activity: Activity             // 最新详情
}

type Notice = { noticeId: string; type: 'venue_changed'|'time_changed'|'cancelled'|'reservation'; activityId: string; message: string; createdAt: string }
```
同一个 (studentId, activityId) 最多一条有效记录。

---

## 6. API [PROPOSED]

所有接口前缀 `/api`；登录用 cookie session；学生只能访问自己的数据，服务端校验。

| 方法 + 路径 | 作用 | 备注 |
|---|---|---|
| `POST /login` / `POST /logout` / `GET /me` | 登录、退出、当前用户 | |
| `GET /activities?q=` | 已发布活动列表 | 只返回 `published`；已取消的活动只在"我的报名"里出现 |
| `GET /activities/:id` | 详情，含 `registrationState`、`myRegistration` | |
| `GET /register/:token` | 解析二维码，返回活动 + 我的报名 | 未登录返回 401 |
| `POST /register/:token` | 确认报名（**幂等**） | 成功 200/201；已报名返回已有记录 |
| `GET /my/registrations` | 我的报名列表 | |
| `GET /my/registrations/:id` | 报名详情 | 非本人返回 404 |
| `POST /my/registrations/:id/cancel` | 取消报名 | 失败 `TOO_LATE` |
| `POST /my/registrations/:id/confirm` / `decline` | 响应预留名额 | |
| `PUT /my/registrations/:id/reminder` | 设置提醒；`{ "reminderAt": null }` 关闭 | 失败 `INVALID_REMINDER_TIME` |
| `GET /my/notices` | 变更/取消通知 | 邮件可模拟为站内通知 |

**错误格式**：`{ "error": "FULL", "message": "名额已满" }`
错误码：`NOT_SIGNED_IN` 401、`NOT_FOUND` 404、`FULL`、`CLOSED`、`CANCELLED`、`NOT_MEMBER`、`TOO_LATE`、`INVALID_REMINDER_TIME`（后几个用 409/422）。

**并发规则**：两人同时抢最后一个名额，只能一人成功，另一人收到 `FULL`，不生成记录——用 SQLite 事务保证。

---

## 7. mock 数据必须覆盖的情形

- 可报名、剩余名额很少（如 1）
- 名额已满
- 已过截止时间；开始前 12 小时内（`closed`）
- 活动已取消（带原因）
- **我已报名**，且活动此时已满
- club-only 且我不是成员
- 报名后地点被改（MyRegistrations 显示新地点 + 通知）
- `reserved` 状态（时间变更待确认）
- 距开始不足 24 小时（取消按钮禁用）
- 提醒：已设置 / 未设置 / 无效时间
- 空列表、搜索无结果、请求失败、加载中
- 标题或地点很长的文字

---

## 8. 验收要点（摘自 US01–US04）

- 手机上无横向滚动（320px 宽、400% 缩放仍可用）。
- 无结果时有明确提示并可清空搜索。
- 成功页面清楚写出"你已报名"；失败写出原因。
- Registered Activities 只显示自己的报名，空状态明确。
- 提醒无效时有解释，可修正，可关闭。
- 无障碍（WCAG 2.2 AA，见 `requirements.md` §7）：每个字段有可见 label、状态不只靠颜色、按钮 ≥44×44px、键盘可操作、焦点可见、错误信息关联到字段。

---

## 9. 待确认 [OPEN]

1. ~~B1 与 User Stories5 冲突~~ **已决定**：以 backlog 为准，**公开浏览，报名才要登录**。
2. 搜索是否要加"按日期筛选"（US01 有，backlog B2 只有关键词）？
3. club-only 活动：对非成员**隐藏**，还是**可见但不能报**？（B7 只说"限制"）
4. 已决定采用站内通知。取消、变更、预约名额释放和到时提醒可在 My registrations 查看。
5. 二维码里的 `token` 用什么？（建议随机字符串，别用自增 ID）
6. B30–B35（时间变更与预留）工作量大（L）且依赖组织者和老师两端，**建议最后做**，先把 B1–B4、B15–B19、B21 主线做稳。
