import os
import shutil
from PIL import Image, ImageDraw

def generate_icons(base_image_path, project_root):
    img = Image.open(base_image_path).convert('RGBA')
    
    # 1. Artwork bounding box in base image: [47, 37, 279, 253]
    art = img.crop((47, 37, 279, 253))
    aw, ah = art.size

    # 2. Master circular icon (512x512)
    master_sz = 512
    # Balanced scaling (340px width fits proportionally inside the circle badge)
    target_w = 340
    target_h = int(target_w * (ah / aw))
    scaled_art = art.resize((target_w, target_h), Image.Resampling.LANCZOS)

    canvas = Image.new('RGBA', (master_sz, master_sz), (247, 247, 247, 255))
    ox = (master_sz - target_w) // 2
    oy = (master_sz - target_h) // 2
    canvas.paste(scaled_art, (ox, oy), mask=scaled_art)

    # Circular mask
    mask = Image.new('L', (master_sz, master_sz), 0)
    d_mask = ImageDraw.Draw(mask)
    d_mask.ellipse((4, 4, master_sz - 5, master_sz - 5), fill=255)

    master_circular = Image.new('RGBA', (master_sz, master_sz), (0, 0, 0, 0))
    master_circular.paste(canvas, (0, 0), mask=mask)

    # Outer border ring matching theme line color (#2D3C69)
    d_res = ImageDraw.Draw(master_circular)
    d_res.ellipse((4, 4, master_sz - 5, master_sz - 5), outline=(45, 60, 105, 255), width=10)

    # 3. Master adaptive foreground (108dp viewport, safe area is inner 66-72dp)
    fg_master_sz = 432
    fg_art_w = 260
    fg_art_h = int(fg_art_w * (ah / aw))
    scaled_fg_art = art.resize((fg_art_w, fg_art_h), Image.Resampling.LANCZOS)
    
    master_fg = Image.new('RGBA', (fg_master_sz, fg_master_sz), (0, 0, 0, 0))
    fg_ox = (fg_master_sz - fg_art_w) // 2
    fg_oy = (fg_master_sz - fg_art_h) // 2
    master_fg.paste(scaled_fg_art, (fg_ox, fg_oy), mask=scaled_fg_art)

    # Target res directory
    res_dir = os.path.join(project_root, 'app', 'src', 'main', 'res')

    # Android density standard sizes
    # (density, legacy/round icon size, adaptive fg size)
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

        # A. ic_launcher.png (Circular badge)
        resized_icon = master_circular.resize((icon_sz, icon_sz), Image.Resampling.LANCZOS)
        resized_icon.save(os.path.join(mipmap_dir, 'ic_launcher.png'))

        # B. ic_launcher_round.png (Circular badge)
        resized_icon.save(os.path.join(mipmap_dir, 'ic_launcher_round.png'))

        # C. ic_launcher_foreground.png (Adaptive icon foreground)
        resized_fg = master_fg.resize((fg_sz, fg_sz), Image.Resampling.LANCZOS)
        resized_fg.save(os.path.join(mipmap_dir, 'ic_launcher_foreground.png'))

        print(f"Generated icons for mipmap-{density} (icon: {icon_sz}x{icon_sz}, fg: {fg_sz}x{fg_sz})")

    # Save a master 512x512 copy in root
    master_circular.save(os.path.join(project_root, 'app_icon_512.png'))
    print("All icons successfully generated!")

if __name__ == '__main__':
    base_img = r'C:\Users\jmustapa\.gemini\antigravity-cli\brain\6b74d58d-c55f-46ba-9155-8da587264e82\.user_uploaded\uploaded_media_1789088923726.png'
    proj_root = r'D:\ai_project\air_mousekey'
    generate_icons(base_img, proj_root)
