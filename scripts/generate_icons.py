import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

def generate_neon_amk_icons(base_image_path, project_root):
    img = Image.open(base_image_path).convert('RGBA')

    # 1. Bounding box of content: [29, 41, 281, 293] (252x252)
    art = img.crop((29, 41, 281, 293))
    arr = np.array(art, dtype=float)
    brightness = np.mean(arr[:, :, :3], axis=2)
    stroke_mask = np.clip((210 - brightness) / 70.0 * 255.0, 0, 255).astype(np.uint8)

    size = 512
    font_path = os.path.join(project_root, 'scripts', 'Orbitron.ttf')

    # 2. Master dark disc background
    bg = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg)
    for r in range(size // 2 - 4, 0, -1):
        ratio = r / (size // 2)
        cr = int(24 * ratio + 10 * (1 - ratio))
        cg = int(20 * ratio + 9 * (1 - ratio))
        cb = int(42 * ratio + 18 * (1 - ratio))
        bg_draw.ellipse((size//2 - r, size//2 - r, size//2 + r, size//2 + r), fill=(cr, cg, cb, 255))

    # Outer neon ring (Cyan top #00F2FE, Purple bottom #E040FB)
    ring = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    ring_draw = ImageDraw.Draw(ring)
    for y in range(size):
        ratio = y / size
        r = int(0 * (1 - ratio) + 224 * ratio)
        g = int(242 * (1 - ratio) + 64 * ratio)
        b = int(254 * (1 - ratio) + 251 * ratio)
        ring_draw.line([(0, y), (size, y)], fill=(r, g, b, 255))

    ring_mask = Image.new('L', (size, size), 0)
    rm_draw = ImageDraw.Draw(ring_mask)
    rm_draw.ellipse((8, 8, size - 9, size - 9), outline=255, width=9)
    bg.paste(ring, (0, 0), mask=ring_mask)

    # Ambient ring glow
    glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse((8, 8, size - 9, size - 9), outline=(0, 242, 254, 100), width=14)
    glow = glow.filter(ImageFilter.GaussianBlur(5))
    bg.paste(glow, (0, 0), mask=glow)

    # 3. Artwork colored with smooth neon gradient (Cyan -> Violet -> Purple)
    h, w = stroke_mask.shape
    grad = np.zeros((h, w, 4), dtype=np.uint8)
    for y in range(h):
        ratio = y / h
        r = int(0 * (1 - ratio) + 217 * ratio)
        g = int(242 * (1 - ratio) + 70 * ratio)
        b = int(254 * (1 - ratio) + 239 * ratio)
        grad[y, :, 0] = r
        grad[y, :, 1] = g
        grad[y, :, 2] = b
        grad[y, :, 3] = stroke_mask[y, :]

    colored_art = Image.fromarray(grad)
    art_size = 290
    scaled_art = colored_art.resize((art_size, art_size), Image.Resampling.LANCZOS)
    ox = (size - art_size) // 2

    # Layout with exact 2px clearance
    # Art bottom is at oy + 289
    # Text glyph top is at text_y + 16
    # 2px clearance: text_y + 16 = oy + 289 + 1 + 2  =>  text_y = oy + 276
    gap_px = 2
    total_group_h = 290 + gap_px + 41  # 333
    oy = (size - total_group_h) // 2   # 89
    text_y = oy + 274 + gap_px         # 365

    # Subtle neon bloom behind artwork
    art_glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    art_glow.paste(scaled_art, (ox, oy), mask=scaled_art)
    art_glow = art_glow.filter(ImageFilter.GaussianBlur(5))
    bg.paste(art_glow, (0, 0), mask=art_glow)
    bg.paste(scaled_art, (ox, oy), mask=scaled_art)

    # 4. Render 'AMK' with Orbitron font (2px distance below artwork)
    font = ImageFont.truetype(font_path, 56)
    text_layer = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    t_draw = ImageDraw.Draw(text_layer)
    letters = ['A', 'M', 'K']
    widths = [t_draw.textbbox((0, 0), l, font=font)[2] - t_draw.textbbox((0, 0), l, font=font)[0] for l in letters]
    gap = 14
    total_w = sum(widths) + gap * (len(letters) - 1)
    cur_x = (size - total_w) // 2
    for l, lw in zip(letters, widths):
        for dx, dy in [(0,0), (1,0), (0,1), (1,1)]:
            t_draw.text((cur_x + dx, text_y + dy), l, font=font, fill=(240, 90, 255, 255))
        cur_x += lw + gap

    # Text glow
    tglow = text_layer.filter(ImageFilter.GaussianBlur(6))
    bg.paste(tglow, (0, 0), mask=tglow)
    bg.paste(text_layer, (0, 0), mask=text_layer)

    # Cut circular master icon
    final_mask = Image.new('L', (size, size), 0)
    f_draw = ImageDraw.Draw(final_mask)
    f_draw.ellipse((2, 2, size - 3, size - 3), fill=255)
    master_circular = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    master_circular.paste(bg, (0, 0), mask=final_mask)

    # 5. Master Adaptive Foreground (108dp viewport, safe area inner 66-72dp)
    fg_master_sz = 432
    fg_art_w = 230
    scaled_fg_art = colored_art.resize((fg_art_w, fg_art_w), Image.Resampling.LANCZOS)
    fg_ox = (fg_master_sz - fg_art_w) // 2
    
    fg_font = ImageFont.truetype(font_path, 44)
    fg_total_group_h = fg_art_w + 2 + 32
    fg_oy = (fg_master_sz - fg_total_group_h) // 2
    fg_text_y = fg_oy + fg_art_w + 2 - 12

    master_fg = Image.new('RGBA', (fg_master_sz, fg_master_sz), (0, 0, 0, 0))
    fg_glow = Image.new('RGBA', (fg_master_sz, fg_master_sz), (0, 0, 0, 0))
    fg_glow.paste(scaled_fg_art, (fg_ox, fg_oy), mask=scaled_fg_art)
    fg_glow = fg_glow.filter(ImageFilter.GaussianBlur(4))
    master_fg.paste(fg_glow, (0, 0), mask=fg_glow)
    master_fg.paste(scaled_fg_art, (fg_ox, fg_oy), mask=scaled_fg_art)

    # AMK text on foreground with 2px gap
    fg_text_layer = Image.new('RGBA', (fg_master_sz, fg_master_sz), (0, 0, 0, 0))
    fg_t_draw = ImageDraw.Draw(fg_text_layer)
    fg_widths = [fg_t_draw.textbbox((0, 0), l, font=fg_font)[2] - fg_t_draw.textbbox((0, 0), l, font=fg_font)[0] for l in letters]
    fg_gap = 11
    fg_total_w = sum(fg_widths) + fg_gap * (len(letters) - 1)
    fg_cur_x = (fg_master_sz - fg_total_w) // 2
    for l, lw in zip(letters, fg_widths):
        for dx, dy in [(0,0), (1,0), (0,1), (1,1)]:
            fg_t_draw.text((fg_cur_x + dx, fg_text_y + dy), l, font=fg_font, fill=(240, 90, 255, 255))
        fg_cur_x += lw + fg_gap

    fg_tglow = fg_text_layer.filter(ImageFilter.GaussianBlur(5))
    master_fg.paste(fg_tglow, (0, 0), mask=fg_tglow)
    master_fg.paste(fg_text_layer, (0, 0), mask=fg_text_layer)

    # Save to res/mipmap folders
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

        resized_icon = master_circular.resize((icon_sz, icon_sz), Image.Resampling.LANCZOS)
        resized_icon.save(os.path.join(mipmap_dir, 'ic_launcher.png'))
        resized_icon.save(os.path.join(mipmap_dir, 'ic_launcher_round.png'))

        resized_fg = master_fg.resize((fg_sz, fg_sz), Image.Resampling.LANCZOS)
        resized_fg.save(os.path.join(mipmap_dir, 'ic_launcher_foreground.png'))
        print(f"Saved mipmap-{density} (icon: {icon_sz}x{icon_sz}, fg: {fg_sz}x{fg_sz})")

    # Save master 512x512
    master_circular.save(os.path.join(project_root, 'app_icon_512.png'))
    print("Master icon and all mipmap icons successfully updated with 2px gap!")

if __name__ == '__main__':
    base_img = r'C:\Users\jmustapa\.gemini\antigravity-cli\brain\6b74d58d-c55f-46ba-9155-8da587264e82\.user_uploaded\uploaded_media_1789089184484.png'
    proj_root = r'D:\ai_project\air_mousekey'
    generate_neon_amk_icons(base_img, proj_root)
