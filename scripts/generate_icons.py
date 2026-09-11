import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def generate_icons(project_root, down_px=22):
    WIDTH, HEIGHT = 512, 512
    CENTER = (256, 256)
    font_path = os.path.join(project_root, 'scripts', 'Orbitron.ttf')

    CYAN = np.array([0, 240, 255], dtype=float)
    PURPLE = np.array([210, 50, 255], dtype=float)

    def get_gradient_color(t):
        t = np.clip(t, 0.0, 1.0)
        col = (1.0 - t) * CYAN + t * PURPLE
        return (int(col[0]), int(col[1]), int(col[2]))

    kw, kh = 140, 168
    m_scale = 1.45
    mw = int(kw * m_scale)
    mh = int(kh * m_scale)
    
    a_k_gap = 36
    total_caps_span = kw * 2 + a_k_gap
    kx_a = CENTER[0] - total_caps_span // 2
    kx_k = kx_a + kw + a_k_gap
    
    my = CENTER[1] - mh // 2 - 8
    mx = CENTER[0] - mw // 2
    ky_caps = (CENTER[1] - kh // 2) + down_px

    master = Image.new('RGBA', (WIDTH, HEIGHT), (10, 12, 22, 255))
    draw = ImageDraw.Draw(master)

    def draw_bold(d, pos, text, font, fill_col, rad=1):
        x, y = pos
        for dx in range(-rad, rad + 1):
            for dy in range(-rad, rad + 1):
                d.text((x + dx, y + dy), text, fill=fill_col, font=font, anchor='mm')

    font_letter = ImageFont.truetype(font_path, int(kh * 0.52))

    total_w = (kx_k + kw) - kx_a
    gx0 = kx_a
    gx1 = kx_a + total_w
    def t_from_x(x):
        return np.clip((x - gx0) / float(gx1 - gx0), 0.0, 1.0)

    # 1. M in background
    col_m = get_gradient_color(t_from_x(mx + mw // 2))
    v_notch_h = int(mh * 0.26)
    pts_m = [
        (mx + 12, my + 50),
        (mx + 34, my + 6),
        (mx + mw//2 - 16, my),
        (mx + mw//2, my + v_notch_h),
        (mx + mw//2 + 16, my),
        (mx + mw - 34, my + 6),
        (mx + mw - 12, my + 50),
        (mx + mw - 6, my + 105),
        (mx + mw - 10, my + mh - 22),
        (mx + mw - 28, my + mh),
        (mx + 28, my + mh),
        (mx + 10, my + mh - 22),
        (mx + 6, my + 105)
    ]
    draw.polygon(pts_m, fill=(16, 20, 36, 245), outline=col_m, width=6)
    draw.line([(mx + mw//2, my + v_notch_h), (mx + mw//2, my + int(mh * 0.54))], fill=col_m, width=4)
    draw.arc([mx + 18, my + int(mh * 0.36), mx + mw - 18, my + int(mh * 0.64)],
             start=200, end=340, fill=col_m, width=3)
             
    wh_w = 18
    wh_h = 36
    wh_x = mx + mw//2 - wh_w//2
    wh_y = my + int(mh * 0.16)
    draw.rounded_rectangle([wh_x, wh_y, wh_x + wh_w, wh_y + wh_h], radius=7, fill=(35, 45, 80, 255), outline=col_m, width=3)
    for y_off in [9, 18, 27]:
        draw.line([(wh_x + 3, wh_y + y_off), (wh_x + wh_w - 2, wh_y + y_off)], fill=(220, 235, 255), width=2)
        
    draw.arc([mx + 38, my + mh - 40, mx + mw - 38, my + mh - 20],
             start=20, end=160, fill=col_m, width=3)

    # 2. Shadow for A & K
    shadow = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    rad = 22
    s_draw.rounded_rectangle([kx_a - 2, ky_caps - 2, kx_a + kw + 2, ky_caps + kh + 2], radius=rad, fill=(0, 0, 0, 220))
    s_draw.rounded_rectangle([kx_k - 2, ky_caps - 2, kx_k + kw + 2, ky_caps + kh + 2], radius=rad, fill=(0, 0, 0, 220))
    s_blur = shadow.filter(ImageFilter.GaussianBlur(8))
    master.alpha_composite(s_blur)
    draw = ImageDraw.Draw(master)

    # 3. Keycap A
    col_a = get_gradient_color(t_from_x(kx_a + kw // 2))
    draw.rounded_rectangle([kx_a, ky_caps, kx_a + kw, ky_caps + kh], radius=rad, fill=(14, 18, 32, 255), outline=col_a, width=6)
    tx0_a = kx_a + 17
    tx1_a = kx_a + kw - 17
    ty0 = ky_caps + 14
    ty1 = ky_caps + kh - 26
    draw.rounded_rectangle([tx0_a, ty0, tx1_a, ty1], radius=18, fill=(20, 26, 46, 220), outline=(col_a[0], col_a[1], col_a[2], 180), width=3)
    draw.arc([tx0_a - 8, ty0 + 4, tx0_a + 20, ty1 - 4], start=270, end=90, fill=col_a, width=3)
    draw.arc([tx1_a - 20, ty0 + 4, tx1_a + 8, ty1 - 4], start=90, end=270, fill=col_a, width=3)
    c_off = 9
    t_off = 4
    draw.line([(kx_a + c_off, ky_caps + c_off), (tx0_a + t_off, ty0 + t_off)], fill=col_a, width=3)
    draw.line([(kx_a + kw - c_off, ky_caps + c_off), (tx1_a - t_off, ty0 + t_off)], fill=col_a, width=3)
    draw.line([(kx_a + c_off, ky_caps + kh - c_off), (tx0_a + t_off, ty1 - t_off)], fill=col_a, width=3)
    draw.line([(kx_a + kw - c_off, ky_caps + kh - c_off), (tx1_a - t_off, ty1 - t_off)], fill=col_a, width=3)
    draw_bold(draw, ((tx0_a + tx1_a) // 2, (ty0 + ty1) // 2), 'A', font_letter, col_a, rad=1)

    # 4. Keycap K
    col_k = get_gradient_color(t_from_x(kx_k + kw // 2))
    draw.rounded_rectangle([kx_k, ky_caps, kx_k + kw, ky_caps + kh], radius=rad, fill=(16, 16, 32, 255), outline=col_k, width=6)
    tx0_k = kx_k + 17
    tx1_k = kx_k + kw - 17
    draw.rounded_rectangle([tx0_k, ty0, tx1_k, ty1], radius=18, fill=(24, 20, 46, 220), outline=(col_k[0], col_k[1], col_k[2], 180), width=3)
    draw.arc([tx0_k - 8, ty0 + 4, tx0_k + 20, ty1 - 4], start=270, end=90, fill=col_k, width=3)
    draw.arc([tx1_k - 20, ty0 + 4, tx1_k + 8, ty1 - 4], start=90, end=270, fill=col_k, width=3)
    draw.line([(kx_k + c_off, ky_caps + c_off), (tx0_k + t_off, ty0 + t_off)], fill=col_k, width=3)
    draw.line([(kx_k + kw - c_off, ky_caps + c_off), (tx1_k - t_off, ty0 + t_off)], fill=col_k, width=3)
    draw.line([(kx_k + c_off, ky_caps + kh - c_off), (tx0_k + t_off, ty1 - t_off)], fill=col_k, width=3)
    draw.line([(kx_k + kw - c_off, ky_caps + kh - c_off), (tx1_k - t_off, ty1 - t_off)], fill=col_k, width=3)
    draw_bold(draw, ((tx0_k + tx1_k) // 2, (ty0 + ty1) // 2), 'K', font_letter, col_k, rad=1)

    master.save(os.path.join(project_root, 'app_icon_512.png'))

    art_crop = master.crop((kx_a - 4, my - 4, kx_k + kw + 4, ky_caps + kh + 4))
    fg_master = Image.new('RGBA', (432, 432), (0, 0, 0, 0))
    scale_fg = 260.0 / art_crop.width
    fg_w = int(art_crop.width * scale_fg)
    fg_h = int(art_crop.height * scale_fg)
    art_scaled = art_crop.resize((fg_w, fg_h), Image.Resampling.LANCZOS)
    fg_ox = (432 - fg_w) // 2
    fg_oy = (432 - fg_h) // 2
    fg_master.paste(art_scaled, (fg_ox, fg_oy), mask=art_scaled)

    legacy_padded = Image.new('RGBA', (512, 512), (10, 12, 22, 255))
    leg_scale = 0.82
    scaled_w = int(512 * leg_scale)
    scaled_h = int(512 * leg_scale)
    scaled_art = master.resize((scaled_w, scaled_h), Image.Resampling.LANCZOS)
    legacy_padded.paste(scaled_art, ((512 - scaled_w)//2, (512 - scaled_h)//2), mask=scaled_art)

    res_dir = os.path.join(project_root, 'app', 'src', 'main', 'res')
    densities = {
        'mdpi': (48, 108),
        'hdpi': (72, 162),
        'xhdpi': (96, 216),
        'xxhdpi': (144, 324),
        'xxxhdpi': (192, 432),
    }

    for density, (icon_sz, fg_sz) in densities.items():
        mipmap_dir = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(mipmap_dir, exist_ok=True)

        resized_icon = legacy_padded.resize((icon_sz, icon_sz), Image.Resampling.LANCZOS)
        resized_icon.save(os.path.join(mipmap_dir, 'ic_launcher.png'))
        resized_icon.save(os.path.join(mipmap_dir, 'ic_launcher_round.png'))

        resized_fg = fg_master.resize((fg_sz, fg_sz), Image.Resampling.LANCZOS)
        resized_fg.save(os.path.join(mipmap_dir, 'ic_launcher_foreground.png'))

    print('Persisted generate_icons.py with G2 down_px=22')

if __name__ == '__main__':
    generate_icons(r'D:i_projectir_mousekey', down_px=22)
