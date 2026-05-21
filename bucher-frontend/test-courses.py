from playwright.sync_api import sync_playwright
import os

screenshot_dir = '/tmp/screenshots'
os.makedirs(screenshot_dir, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Step 1: Navigate to home, should redirect to login (no token)
    page.goto('http://localhost:5173/')
    page.wait_for_load_state('networkidle')
    page.screenshot(path=f'{screenshot_dir}/01-login-redirect.png', full_page=True)
    print('Step 1: Redirected to login (no token)')

    # Step 2: Set fake token and navigate home
    page.evaluate('localStorage.setItem("token", "fake-token")')
    # Also set a minimal userInfo so the header renders
    page.evaluate('localStorage.setItem("userInfo", JSON.stringify({id:1,realName:"测试用户",role:1,roleName:"教师",userNo:"T001",email:"test@test.com",phone:null,avatarUrl:null,deptId:null,deptName:null,adminClassId:null,adminClassName:null,source:1,auditStatus:null,status:1,createTime:"2025-01-01"}))')
    page.goto('http://localhost:5173/')
    page.wait_for_load_state('networkidle')
    page.screenshot(path=f'{screenshot_dir}/02-home.png', full_page=True)
    print('Step 2: Home page loaded')

    # Check home page has 我的课程 card
    course_card = page.locator('text=我的课程').first
    if course_card.is_visible():
        print('  -> "我的课程" card is visible on home page')
    else:
        print('  -> ERROR: "我的课程" card NOT found')

    # Step 3: Navigate to /courses
    page.goto('http://localhost:5173/courses')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(500)
    page.screenshot(path=f'{screenshot_dir}/03-course-list.png', full_page=True)
    print('Step 3: Course list page loaded')

    # Verify 6 course cards
    course_names = ['高等数学', '大学物理', '程序设计基础', '数据结构', '操作系统', '计算机网络']
    for name in course_names:
        if page.locator(f'text={name}').first.is_visible():
            print(f'  -> Found course: {name}')
        else:
            print(f'  -> ERROR: Course NOT found: {name}')

    # Step 4: Click the first course card (高等数学)
    page.locator('text=高等数学').first.click()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(500)
    page.screenshot(path=f'{screenshot_dir}/04-course-detail.png', full_page=True)
    print('Step 4: Course detail page loaded')

    # Check URL
    current_url = page.url
    print(f'  -> Current URL: {current_url}')
    if '/courses/1' in current_url and 'name=' in current_url:
        print('  -> URL contains course id and query params')
    else:
        print('  -> ERROR: URL does not contain expected params')

    # Step 5: Check header shows course info instead of logo
    header_text = page.locator('.course-name-header').text_content()
    if '高等数学' in header_text:
        print(f'  -> Header shows course name: {header_text}')
    else:
        print(f'  -> ERROR: Header course name not found, got: {header_text}')

    teacher_text = page.locator('.course-meta-header').first.text_content()
    print(f'  -> Header shows teacher: {teacher_text}')

    # Step 6: Check sidebar has 4 menu items
    sidebar_items = ['课程章节', '作业', '实验', '资料']
    for item in sidebar_items:
        nav_item = page.locator(f'.nav-item:has-text("{item}")')
        if nav_item.count() > 0:
            print(f'  -> Sidebar item found: {item}')
        else:
            print(f'  -> ERROR: Sidebar item NOT found: {item}')

    # Step 7: Click 作业 in sidebar
    page.locator('.nav-item:has-text("作业")').click()
    page.wait_for_timeout(300)
    page.screenshot(path=f'{screenshot_dir}/05-homework-tab.png', full_page=True)
    placeholder = page.locator('.placeholder-text').text_content()
    print(f'Step 7: Homework tab - placeholder: {placeholder}')
    if '作业' in placeholder:
        print('  -> Homework placeholder correct')
    else:
        print(f'  -> ERROR: Unexpected placeholder: {placeholder}')

    # Step 8: Click 实验
    page.locator('.nav-item:has-text("实验")').click()
    page.wait_for_timeout(300)
    page.screenshot(path=f'{screenshot_dir}/06-lab-tab.png', full_page=True)
    placeholder = page.locator('.placeholder-text').text_content()
    print(f'Step 8: Lab tab - placeholder: {placeholder}')
    if '实验' in placeholder:
        print('  -> Lab placeholder correct')
    else:
        print(f'  -> ERROR: Unexpected placeholder: {placeholder}')

    # Also click 资料
    page.locator('.nav-item:has-text("资料")').click()
    page.wait_for_timeout(300)
    placeholder = page.locator('.placeholder-text').text_content()
    print(f'  -> Material tab - placeholder: {placeholder}')

    # Step 9: Click back button
    page.locator('.back-btn').click()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(500)
    page.screenshot(path=f'{screenshot_dir}/07-back-to-list.png', full_page=True)
    current_url = page.url
    print(f'Step 9: After back button - URL: {current_url}')
    if current_url == 'http://localhost:5173/courses' or current_url.endswith('/courses'):
        print('  -> Successfully returned to course list')
    else:
        print(f'  -> ERROR: Did not return to course list')

    # Step 10: Go to home page via header logo click
    page.goto('http://localhost:5173/')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(500)
    page.screenshot(path=f'{screenshot_dir}/08-home-final.png', full_page=True)
    print('Step 10: Home page')
    if page.locator('text=我的课程').first.is_visible():
        print('  -> "我的课程" card visible on home page')

    # Test navigation via header dropdown
    page.goto('http://localhost:5173/')
    page.wait_for_load_state('networkidle')
    # Click user avatar dropdown trigger
    page.locator('.user-trigger').click()
    page.wait_for_timeout(300)
    page.screenshot(path=f'{screenshot_dir}/09-dropdown.png', full_page=True)
    # Click 我的课程 in dropdown
    page.locator('.menu-item:has-text("我的课程")').click()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(500)
    current_url = page.url
    print(f'Step 11: Navigated via dropdown - URL: {current_url}')
    if '/courses' in current_url:
        print('  -> Dropdown navigation to courses works')

    print('\n=== All tests completed ===')
    browser.close()
