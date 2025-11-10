"use client";
import { useEffect, useRef, useState } from "react";
import { Color, Vector3 } from "three";
import ThreeGlobe from "three-globe";
import * as THREE from 'three';
import { useThree, Canvas, extend, useLoader, useFrame } from "@react-three/fiber";
import { OrbitControls, Html, Effects } from "@react-three/drei";
import { UnrealBloomPass, KTX2Loader } from 'three-stdlib';
import countries from "@/data/globe.json";
declare module "@react-three/fiber" {
  interface ThreeElements {
    threeGlobe: ThreeElements["mesh"] & {
      new (): ThreeGlobe;
    };
  }
}

let __ktx2Loader: KTX2Loader | null = null;
function getKtx2Loader(gl: THREE.WebGLRenderer): KTX2Loader {
  if (!__ktx2Loader) {
    __ktx2Loader = new KTX2Loader().setTranscoderPath('/basis/');
    __ktx2Loader.detectSupport(gl);
  }
  return __ktx2Loader;
}

const RING_PROPAGATION_SPEED = 3;

type Position = {
  order: number;
  startLat: number;
  startLng: number;
  endLat: number;
  endLng: number;
  arcAlt: number;
  color: string;
};

// type Arc = {
//   startLat: number;
//   startLng: number;
//   endLat: number;
//   endLng: number;
//   color: string;
//   arcAlt: number;
//   order: number;
// };

export type GlobeConfig = {
  pointSize?: number;
  globeColor?: string;
  globeImageUrl?: string;
  bumpImageUrl?: string;
  specularImageUrl?: string;
  cloudsImageUrl?: string;
  cloudsSpeed?: number;
  nightImageUrl?: string;
  showNightLights?: boolean;
  emissive?: string;
  emissiveIntensity?: number;
  shininess?: number;
  polygonColor?: string;
  showHexPolygons?: boolean;
  ambientLight?: string;
  directionalLeftLight?: string;
  directionalTopLight?: string;
  pointLight?: string;
  ambientIntensity?: number;
  directionalIntensity?: number;
  arcDensity?: number;      // 0..1 fraction of arcs to render
  maxArcs?: number;         // hard cap on number of arcs to render
  // Arc animation controls (moving arcs)
  arcTime?: number;         // total time to animate dash (ms)
  arcLength?: number;       // dash length portion (0..1); <1 enables motion
  arcAnimate?: boolean;     // enable moving dash animation
  arcGap?: number;          // dash gap portion (0..1)
  rings?: number;
  maxRings?: number;
  // Toggle animated pulse rings at arc endpoints
  showRings?: boolean;
  initialPosition?: {
    lat: number;
    lng: number;
    altitude?: number;
  };
  autoRotate?: boolean;
  autoRotateSpeed?: number;
  flipPoles?: boolean;
  flipTextureVertically?: boolean;
  flipTextureHorizontally?: boolean;
  flipLongitude?: boolean;
  // Background
  useSkybox?: boolean;
  starsBackgroundUrl?: string;
  starfieldCount?: number;
  // Postprocessing
  enableBloom?: boolean;
  // When true, keeps bloom active even in development (not recommended)
  forceBloomInDev?: boolean;
};

interface WorldProps {
  globeConfig: GlobeConfig;
  data: Position[];
}

export function Globe({ globeConfig, data }: WorldProps) {
  const { gl } = useThree();
  const globeRef = useRef<ThreeGlobe | null>(null);
  const groupRef = useRef<THREE.Group | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const specularTexRef = useRef<THREE.Texture | null>(null);
  const dayTexRef = useRef<THREE.Texture | null>(null);
  const bumpTexRef = useRef<THREE.Texture | null>(null);
  const ktx2Ref = useRef<KTX2Loader | null>(null);

  const defaultProps = {
    pointSize: 1,
    polygonColor: "rgba(255,255,255,0.7)",
    globeColor: "#1d072e",
    globeImageUrl: undefined as string | undefined,
    bumpImageUrl: undefined as string | undefined,
    specularImageUrl: undefined as string | undefined,
    nightImageUrl: "/earth_nightmap.jpg" as string | undefined,
    cloudsImageUrl: undefined as string | undefined,
    cloudsSpeed: 0.0025,
    emissive: "#000000",
    emissiveIntensity: 0.1,
    shininess: 20,
    arcDensity: 0.2,
  maxArcs: undefined as number | undefined,
    // Moving arc defaults (tasteful glint)
    arcTime: 2000,
    arcLength: 0.25,
    arcAnimate: true,
    arcGap: 0.95,
  rings: 1,
  maxRings: 3,
  showRings: true,
  flipPoles: false,
  flipTextureVertically: false,
  flipTextureHorizontally: false,
  flipLongitude: false,
    showHexPolygons: false,
    useSkybox: true,
    starsBackgroundUrl: "/stars_milky.jpg",
    starfieldCount: 2000,
    enableBloom: true,
    forceBloomInDev: false,
    ...globeConfig,
  };

  // Initialize globe only once
  useEffect(() => {
    if (!globeRef.current && groupRef.current) {
      globeRef.current = new ThreeGlobe();
      groupRef.current.add(globeRef.current);
      setIsInitialized(true);
    }
  }, []);

  // Ensure built-in atmosphere is disabled at init
  useEffect(() => {
    if (!globeRef.current || !isInitialized) return;
    globeRef.current.showAtmosphere(false);
  }, [isInitialized]);

  // Build material when globe is initialized or when relevant props change
  useEffect(() => {
    if (!globeRef.current || !isInitialized) return;

    const globeMaterial = globeRef.current.globeMaterial() as unknown as {
      color: Color;
      emissive: Color;
      specular?: Color;
  specularMap?: THREE.Texture | null;
  map?: THREE.Texture | null;
  bumpMap?: THREE.Texture | null;
  bumpScale?: number;
  needsUpdate?: boolean;
      emissiveIntensity: number;
      shininess: number;
    };
    const useTex = !!defaultProps.globeImageUrl;
    const baseColor = useTex ? "#ffffff" : globeConfig.globeColor;
    globeMaterial.color = new Color(baseColor);
    globeMaterial.emissive = new Color(globeConfig.emissive);
    globeMaterial.emissiveIntensity = globeConfig.emissiveIntensity || 0.1;
    globeMaterial.shininess = globeConfig.shininess || 20;

    // Prepare KTX2 loader once
    if (!ktx2Ref.current) {
  try { ktx2Ref.current = getKtx2Loader(gl as unknown as THREE.WebGLRenderer); } catch {}
    }

    const applyTextureFlip = (tex: THREE.Texture) => {
      const flipX = defaultProps.flipTextureHorizontally ?? false;
      const flipY = defaultProps.flipTextureVertically ?? false;
      tex.repeat.set(flipX ? -1 : 1, flipY ? -1 : 1);
      tex.offset.set(flipX ? 1 : 0, flipY ? 1 : 0);
      tex.needsUpdate = true;
    };

    const loadKTX2 = (url: string, onLoad: (t: THREE.Texture) => void, onError?: () => void) => {
      if (!ktx2Ref.current) return onError?.();
      ktx2Ref.current.load(url, (tex) => {
        tex.wrapS = THREE.ClampToEdgeWrapping;
        tex.wrapT = THREE.ClampToEdgeWrapping;
        applyTextureFlip(tex);
        onLoad(tex);
      }, undefined, () => onError?.());
    };

    // Apply globe (day) texture
    if (defaultProps.globeImageUrl) {
      if (defaultProps.globeImageUrl.toLowerCase().endsWith('.ktx2')) {
        try { dayTexRef.current?.dispose(); } catch {}
        loadKTX2(defaultProps.globeImageUrl, (tex) => {
          // Ensure proper color space so colors don't wash out
          tex.colorSpace = THREE.SRGBColorSpace;
          const hasMipmaps = Array.isArray(tex.mipmaps) && tex.mipmaps.length > 1;
          tex.generateMipmaps = false;
          tex.minFilter = hasMipmaps ? THREE.LinearMipmapLinearFilter : THREE.LinearFilter;
          tex.magFilter = THREE.LinearFilter;
          dayTexRef.current = tex;
          globeMaterial.map = tex;
          globeMaterial.needsUpdate = true;
        }, () => {
          globeRef.current!.globeImageUrl(defaultProps.globeImageUrl!);
        });
      } else {
        globeRef.current.globeImageUrl(defaultProps.globeImageUrl);
      }
    }

    // Apply bump texture
    if (defaultProps.bumpImageUrl) {
      if (defaultProps.bumpImageUrl.toLowerCase().endsWith('.ktx2')) {
        try { bumpTexRef.current?.dispose(); } catch {}
        loadKTX2(defaultProps.bumpImageUrl, (tex) => {
          bumpTexRef.current = tex;
          globeMaterial.bumpMap = tex;
          globeMaterial.bumpScale = 0.4;
          globeMaterial.needsUpdate = true;
        }, () => {
          globeRef.current!.bumpImageUrl(defaultProps.bumpImageUrl!);
        });
      } else {
        globeRef.current.bumpImageUrl(defaultProps.bumpImageUrl);
      }
    }

    // Optional specular map to enhance ocean highlights
    if (defaultProps.specularImageUrl) {
      // Dispose the previous one if any
      try { specularTexRef.current?.dispose(); } catch {}
      const onSpecularReady = (tex: THREE.Texture) => {
        tex.colorSpace = THREE.SRGBColorSpace;
        tex.wrapS = THREE.ClampToEdgeWrapping;
        tex.wrapT = THREE.ClampToEdgeWrapping;
        const hasMipmaps = Array.isArray(tex.mipmaps) && tex.mipmaps.length > 1;
        tex.generateMipmaps = false;
        tex.minFilter = hasMipmaps ? THREE.LinearMipmapLinearFilter : THREE.LinearFilter;
        tex.magFilter = THREE.LinearFilter;
        globeMaterial.specularMap = tex;
        globeMaterial.specular = new THREE.Color(0x444444);
        globeMaterial.needsUpdate = true;
        specularTexRef.current = tex;
      };
      if (defaultProps.specularImageUrl.toLowerCase().endsWith('.ktx2')) {
        loadKTX2(defaultProps.specularImageUrl, onSpecularReady, () => {
          new THREE.TextureLoader().load(defaultProps.specularImageUrl!, onSpecularReady, undefined, () => {
            globeMaterial.specularMap = null;
          });
        });
      } else {
        new THREE.TextureLoader().load(defaultProps.specularImageUrl, onSpecularReady, undefined, () => {
          globeMaterial.specularMap = null;
        });
      }
    } else {
      globeMaterial.specularMap = null;
    }
  }, [
    isInitialized,
    globeConfig.globeColor,
    globeConfig.emissive,
    globeConfig.emissiveIntensity,
    globeConfig.shininess,
    defaultProps.globeImageUrl,
    defaultProps.bumpImageUrl,
    defaultProps.specularImageUrl,
    defaultProps.flipTextureHorizontally,
    defaultProps.flipTextureVertically,
    gl,
  ]);

  // Dispose specular map on unmount
  useEffect(() => {
    return () => {
      try { specularTexRef.current?.dispose(); } catch {}
      try { dayTexRef.current?.dispose(); } catch {}
      try { bumpTexRef.current?.dispose(); } catch {}
      specularTexRef.current = null;
      dayTexRef.current = null;
      bumpTexRef.current = null;
    };
  }, []);

  // One-time orientation to focus the Western Hemisphere by default
  useEffect(() => {
    if (!globeRef.current || !isInitialized) return;
    const lat = defaultProps.initialPosition?.lat ?? 0;
    const lng = defaultProps.initialPosition?.lng ?? 0;
    const altitude = defaultProps.initialPosition?.altitude ?? 2.1;
    requestAnimationFrame(() => {
      try {
        // @ts-expect-error pointOfView exists on three-globe instance but is missing in its type defs
        globeRef.current?.pointOfView({ lat, lng, altitude }, 0);
      } catch {}
    });
    if (groupRef.current) {
      groupRef.current.rotation.set(0, 0, 0);
      if (defaultProps.flipLongitude) {
        groupRef.current.rotation.y = Math.PI;
      }
      if (defaultProps.flipPoles) {
        groupRef.current.rotation.z = Math.PI;
      }
    }
  }, [isInitialized, defaultProps.initialPosition?.lat, defaultProps.initialPosition?.lng, defaultProps.initialPosition?.altitude, defaultProps.flipPoles, defaultProps.flipLongitude]);

  // Build data when globe is initialized or when data changes
  useEffect(() => {
    if (!globeRef.current || !isInitialized || !data) return;

    // Reduce arc frequency by sampling based on arcDensity and optional cap
    const density = Math.min(Math.max(defaultProps.arcDensity ?? 0.2, 0), 1);
    let arcs: Position[] = [];
    if (density > 0 && data.length > 0) {
      const step = Math.max(1, Math.round(1 / density));
      const pre = data.filter((_, i) => i % step === 0);
      arcs = defaultProps.maxArcs != null ? pre.slice(0, defaultProps.maxArcs) : pre;
    }
    const points = [];
    for (let i = 0; i < arcs.length; i++) {
      const arc = arcs[i];
      points.push({
        size: defaultProps.pointSize,
        order: arc.order,
        color: arc.color,
        lat: arc.startLat,
        lng: arc.startLng,
      });
      points.push({
        size: defaultProps.pointSize,
        order: arc.order,
        color: arc.color,
        lat: arc.endLat,
        lng: arc.endLng,
      });
    }

    // remove duplicates for same lat and lng
    const filteredPoints = points.filter(
      (v, i, a) =>
        a.findIndex((v2) =>
          ["lat", "lng"].every(
            (k) => v2[k as "lat" | "lng"] === v[k as "lat" | "lng"],
          ),
        ) === i,
    );

    if (defaultProps.showHexPolygons) {
      globeRef.current
        .hexPolygonsData(countries.features)
        .hexPolygonResolution(3)
        .hexPolygonMargin(0.7)
        .hexPolygonColor(() => defaultProps.polygonColor);
    } else {
      globeRef.current
        .hexPolygonsData([]);
    }

    globeRef.current
      .arcsData(arcs)
      .arcStartLat((d) => (d as { startLat: number }).startLat * 1)
      .arcStartLng((d) => (d as { startLng: number }).startLng * 1)
      .arcEndLat((d) => (d as { endLat: number }).endLat * 1)
      .arcEndLng((d) => (d as { endLng: number }).endLng * 1)
      // @ts-expect-error typings don't reflect gradient accessor by data; runtime supports [start,end]
      .arcColor((d: Position) => [d.color, 'rgba(255,255,255,0)'])
      .arcAltitude((e) => (e as { arcAlt: number }).arcAlt * 1)
      .arcStroke(() => 1.0)
      // Animate dash to create motion along the arc when enabled
      .arcDashLength(defaultProps.arcAnimate ? Math.min(Math.max(defaultProps.arcLength ?? 0.25, 0.01), 0.99) : 1)
      .arcDashInitialGap((e) => (defaultProps.arcAnimate ? (e as { order: number }).order * 1 : 0))
      .arcDashGap(defaultProps.arcAnimate ? Math.min(Math.max(defaultProps.arcGap ?? 0.95, 0), 2) : 0)
      .arcDashAnimateTime(defaultProps.arcAnimate ? (defaultProps.arcTime ?? 2000) : 0);

    globeRef.current
      .pointsData(filteredPoints)
      .pointColor((e) => (e as { color: string }).color)
      .pointsMerge(true)
      .pointAltitude(0.0)
      // Tiny dots at arc starts/ends
      .pointRadius(0.25);

    // Initialize ring settings (cleared by default). If showRings is false, we won't animate/update them later.
    globeRef.current
      .ringsData([])
      .ringColor(() => defaultProps.polygonColor)
      .ringMaxRadius(defaultProps.maxRings)
      .ringPropagationSpeed(RING_PROPAGATION_SPEED)
      .ringRepeatPeriod(
        (defaultProps.arcTime * defaultProps.arcLength) / Math.max(defaultProps.rings, 1),
      );
  }, [
    isInitialized,
    data,
    defaultProps.pointSize,
    defaultProps.polygonColor,
    defaultProps.arcLength,
    defaultProps.arcTime,
    defaultProps.rings,
    defaultProps.maxRings,
  ]);

  // Handle rings animation with cleanup
  useEffect(() => {
    if (!globeRef.current || !isInitialized || !data) return;
    if (!defaultProps.showRings) {
      // Ensure no rings are displayed and skip scheduling
      globeRef.current.ringsData([]);
      return;
    }

    const interval = setInterval(() => {
      if (!globeRef.current) return;

      const newNumbersOfRings = genRandomNumbers(
        0,
        data.length,
        Math.floor((data.length * 4) / 5),
      );

      const ringsData = data
        .filter((d, i) => newNumbersOfRings.includes(i))
        .map((d) => ({
          lat: d.startLat,
          lng: d.startLng,
          color: d.color,
        }));

      globeRef.current.ringsData(ringsData);
    }, 2000);

    return () => {
      clearInterval(interval);
    };
  }, [isInitialized, data, defaultProps.showRings]);

  return <group ref={groupRef} />;
}

export function WebGLRendererConfig() {
  const { gl, size } = useThree();

  useEffect(() => {
    // Cap DPR to reduce GPU memory pressure and avoid WebGL context loss
    gl.setPixelRatio(1);
    gl.setSize(size.width, size.height);
    // Make canvas fully transparent so it blends with page background without edge artifacts
    gl.setClearColor(0x000000, 0);

    // Guard against WebGL context loss; allow the browser to attempt restore
    const canvas = gl.domElement as HTMLCanvasElement;
    const onLost = (e: Event) => {
      e.preventDefault();
      // three/fiber will re-render on restore automatically
    };
    const onRestored = () => {
      // no-op, R3F should resume
    };
    canvas.addEventListener('webglcontextlost', onLost as EventListener, false);
    canvas.addEventListener('webglcontextrestored', onRestored as EventListener, false);
    return () => {
      canvas.removeEventListener('webglcontextlost', onLost as EventListener, false);
      canvas.removeEventListener('webglcontextrestored', onRestored as EventListener, false);
    };
  }, [gl, size.height, size.width]);

  return null;
}

export function World(props: WorldProps) {
  const { globeConfig } = props;
  const worldCfg = { enableBloom: true, forceBloomInDev: false, ...globeConfig } as GlobeConfig;
  const scene = new THREE.Scene();
  scene.fog = null;
  const isProd = process.env.NODE_ENV === 'production';
  return (
    <Canvas
      scene={scene}
      camera={{ fov: 50, near: 180, far: 3000, position: [0, 0, 300] }}
      dpr={[1, 1]}
      // Enable antialiasing and transparent background to avoid a dark fringe around the globe on some GPUs/browsers
      gl={{ powerPreference: 'high-performance', antialias: true, alpha: true, stencil: false, premultipliedAlpha: false, preserveDrawingBuffer: false }}
    >
      <WebGLRendererConfig />
  <ambientLight color={globeConfig.ambientLight} intensity={globeConfig.ambientIntensity ?? 0.35} />
      <directionalLight
        color={globeConfig.directionalLeftLight}
        position={new Vector3(-400, 100, 400)}
        intensity={globeConfig.directionalIntensity ?? 1.5}
      />
      {/* Background */}
      {globeConfig.useSkybox ? (
        <SkySphere textureUrl={globeConfig.starsBackgroundUrl} radius={2200} />
      ) : (
        <Starfield count={globeConfig.starfieldCount ?? 2000} />
      )}
      {/* Atmosphere removed */}
      {/* Night-side city lights (requires a night lights texture) */}
      {globeConfig.showNightLights !== false && globeConfig.nightImageUrl && (
        <NightLights
          nightImageUrl={globeConfig.nightImageUrl}
          lightDir={new Vector3(-400, 100, 400)}
          flipTextureVertically={globeConfig.flipTextureVertically}
          flipTextureHorizontally={globeConfig.flipTextureHorizontally}
        />
      )}
      {/* Cloud layer */}
      {globeConfig.cloudsImageUrl && (
        <Clouds
          cloudsImageUrl={globeConfig.cloudsImageUrl}
          speed={globeConfig.cloudsSpeed ?? 0.0025}
          flipTextureVertically={globeConfig.flipTextureVertically}
          flipTextureHorizontally={globeConfig.flipTextureHorizontally}
        />
      )}
      <Globe {...props} />
      <HoverMarkers data={props.data} />
      {/* Subtle bloom for arcs/city lights (disabled by default in dev to reduce GPU pressure) */}
      {(worldCfg.enableBloom !== false) && (process.env.NODE_ENV !== 'development' || worldCfg.forceBloomInDev) && (
        <Effects disableGamma>
          <primitive object={new UnrealBloomPass(new THREE.Vector2(1024, 1024), 0.4, 0.4, 0.9)} />
        </Effects>
      )}
      <OrbitControls
        enablePan={false}
        enableZoom={true}
        minDistance={220}
        maxDistance={600}
        autoRotateSpeed={globeConfig.autoRotateSpeed ?? 1.2}
        autoRotate={globeConfig.autoRotate !== false}
        enableDamping
        dampingFactor={0.06}
        minPolarAngle={Math.PI / 3.5}
        maxPolarAngle={Math.PI - Math.PI / 3}
      />
    </Canvas>
  );
}

// Subtle atmospheric glow using a BackSide sphere and additive blending
function Atmosphere({ color = "#66a6ff", intensity = 0.25, radius = 102 }: { color?: string; intensity?: number; radius?: number }) {
  const glowColor = new THREE.Color(color);
  const uniforms: { glowColor: { value: THREE.Vector3 }; glowIntensity: { value: number } } = {
    glowColor: { value: new THREE.Vector3(glowColor.r, glowColor.g, glowColor.b) },
    glowIntensity: { value: intensity },
  };
  return (
    <mesh>
      <sphereGeometry args={[radius, 48, 48]} />
      <shaderMaterial
        transparent
        depthWrite={false}
        blending={THREE.AdditiveBlending}
        side={THREE.BackSide}
        uniforms={uniforms}
        vertexShader={`
          varying vec3 vNormal;
          void main() {
            vNormal = normalize(normalMatrix * normal);
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
          }
        `}
        fragmentShader={`
          uniform vec3 glowColor;
          uniform float glowIntensity;
          varying vec3 vNormal;
          void main() {
            float a = pow(1.0 - max(dot(vNormal, vec3(0.0, 0.0, 1.0)), 0.0), 3.0);
            vec3 c = glowColor;
            gl_FragColor = vec4(c * glowIntensity * a, a);
          }
        `}
      />
    </mesh>
  );
}

// Procedural starfield background
function Starfield({ count = 2000, radius = 1200 }: { count?: number; radius?: number }) {
  const geomRef = useRef<THREE.BufferGeometry | null>(null);
  useEffect(() => {
    const geometry = geomRef.current;
    const positions = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
      const r = radius * (0.8 + 0.2 * Math.random());
      const theta = Math.acos(2 * Math.random() - 1);
      const phi = 2 * Math.PI * Math.random();
      const x = r * Math.sin(theta) * Math.cos(phi);
      const y = r * Math.sin(theta) * Math.sin(phi);
      const z = r * Math.cos(theta);
      positions[i * 3 + 0] = x;
      positions[i * 3 + 1] = y;
      positions[i * 3 + 2] = z;
    }
    if (geometry) {
      geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    }
    return () => {
      geometry?.dispose();
    };
  }, [count, radius]);
  return (
    <points>
      <bufferGeometry ref={geomRef} />
      <pointsMaterial color={0xffffff} size={0.9} sizeAttenuation transparent opacity={0.9} />
    </points>
  );
}

// Sky sphere background using a texture (e.g., Milky Way)
function SkySphere({ textureUrl = "/stars_milky.jpg", radius = 1800 }: { textureUrl?: string; radius?: number }) {
  const texture = useLoader(THREE.TextureLoader, textureUrl);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.minFilter = THREE.LinearMipmapLinearFilter;
  texture.generateMipmaps = true;
  useEffect(() => {
    return () => {
      try { texture.dispose(); } catch {}
    };
  }, [texture]);
  return (
    <mesh>
      <sphereGeometry args={[radius, 60, 60]} />
      <meshBasicMaterial map={texture} side={THREE.BackSide} depthWrite={false} />
    </mesh>
  );
}

export function hexToRgb(hex: string) {
  const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
  hex = hex.replace(shorthandRegex, function (m, r, g, b) {
    return r + r + g + g + b + b;
  });

  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result
    ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16),
      }
    : null;
}

export function genRandomNumbers(min: number, max: number, count: number) {
  const arr = [];
  while (arr.length < count) {
    const r = Math.floor(Math.random() * (max - min)) + min;
    if (arr.indexOf(r) === -1) arr.push(r);
  }

  return arr;
}

// Utility to convert lat/lng to 3D position on sphere
function latLngToVec3(radius: number, lat: number, lng: number) {
  const phi = THREE.MathUtils.degToRad(90 - lat);
  const theta = THREE.MathUtils.degToRad(lng + 180);
  const x = -radius * Math.sin(phi) * Math.cos(theta);
  const z = radius * Math.sin(phi) * Math.sin(theta);
  const y = radius * Math.cos(phi);
  return new THREE.Vector3(x, y, z);
}

// Lightweight hover markers with HTML tooltips
function HoverMarkers({ data, radius = 100 }: { data: Position[]; radius?: number }) {
  const [hover, setHover] = useState<{ pos: [number, number, number]; text: string } | null>(null);
  return (
    <group>
      {data.map((d, i) => {
        const p = latLngToVec3(radius + 0.6, d.startLat, d.startLng);
        const label = `lat ${d.startLat.toFixed(2)}, lng ${d.startLng.toFixed(2)}`;
        return (
          <mesh
            key={`m-${i}`}
            position={p}
            onPointerOver={(e) => {
              e.stopPropagation();
              setHover({ pos: [p.x, p.y, p.z], text: label });
            }}
            onPointerOut={(e) => {
              e.stopPropagation();
              setHover((h) => (h && h.text === label ? null : h));
            }}
          >
            <sphereGeometry args={[0.3, 8, 8]} />
            <meshBasicMaterial color={d.color} transparent opacity={0.15} depthWrite={false} />
          </mesh>
        );
      })}
      {hover && (
        <Html position={hover.pos} style={{ pointerEvents: 'none' }}>
          <div style={{ background: 'rgba(0,0,0,0.7)', color: '#fff', padding: '4px 6px', borderRadius: 4, fontSize: 12 }}>
            {hover.text}
          </div>
        </Html>
      )}
    </group>
  );
}

// Night city lights overlay masked to the dark side
function NightLights({ nightImageUrl, lightDir, flipTextureVertically, flipTextureHorizontally }: { nightImageUrl?: string; lightDir: Vector3; flipTextureVertically?: boolean; flipTextureHorizontally?: boolean }) {
  const { gl } = useThree();
  const [texture, setTexture] = useState<THREE.Texture | undefined>(undefined);
  useEffect(() => {
    let disposed = false;
    let loader: KTX2Loader | THREE.TextureLoader | undefined;
    let loadedTexture: THREE.Texture | undefined;
    if (!nightImageUrl) {
      setTexture(undefined);
      return;
    }
    const done = (tex: THREE.Texture) => {
      if (disposed) { try { tex.dispose(); } catch {} return; }
      tex.colorSpace = THREE.SRGBColorSpace;
      const hasMipmaps = Array.isArray(tex.mipmaps) && tex.mipmaps.length > 1;
      tex.generateMipmaps = false;
      tex.minFilter = hasMipmaps ? THREE.LinearMipmapLinearFilter : THREE.LinearFilter;
      tex.magFilter = THREE.LinearFilter;
      const flipX = flipTextureHorizontally ?? false;
      const flipY = flipTextureVertically ?? false;
      tex.repeat.set(flipX ? -1 : 1, flipY ? -1 : 1);
      tex.offset.set(flipX ? 1 : 0, flipY ? 1 : 0);
      tex.needsUpdate = true;
      loadedTexture = tex;
      setTexture(tex);
    };
    if (nightImageUrl.toLowerCase().endsWith('.ktx2')) {
      loader = getKtx2Loader(gl as unknown as THREE.WebGLRenderer);
      loader.load(nightImageUrl, done, undefined, () => setTexture(undefined));
    } else {
      loader = new THREE.TextureLoader();
      loader.load(nightImageUrl, done, undefined, () => setTexture(undefined));
    }
    return () => {
      disposed = true;
      try { loadedTexture?.dispose(); } catch {}
    };
  }, [nightImageUrl, gl, flipTextureVertically, flipTextureHorizontally]);
  if (!texture) return null;
  const uniforms: { map: { value: THREE.Texture }; lightDir: { value: THREE.Vector3 }; threshold: { value: number }; intensity: { value: number } } = {
    map: { value: texture },
    lightDir: { value: lightDir.clone().normalize() },
    threshold: { value: 0.05 },
    intensity: { value: 1.2 },
  };
  return (
    <mesh>
      <sphereGeometry args={[100.6, 48, 48]} />
      <shaderMaterial
        transparent
        depthWrite={false}
        blending={THREE.AdditiveBlending}
        uniforms={uniforms}
        vertexShader={`
          varying vec2 vUv;
          varying vec3 vNormalWorld;
          void main() {
            vUv = uv;
            vNormalWorld = normalize(mat3(modelMatrix) * normal);
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
          }
        `}
        fragmentShader={`
          uniform sampler2D map;
          uniform vec3 lightDir;
          uniform float threshold;
          uniform float intensity;
          varying vec2 vUv;
          varying vec3 vNormalWorld;
          void main() {
            vec3 L = normalize(-lightDir);
            float ndl = dot(normalize(vNormalWorld), L);
            if (ndl > threshold) discard;
            vec4 tex = texture2D(map, vUv);
            vec3 warmed = tex.rgb * vec3(1.2, 1.05, 0.8);
            gl_FragColor = vec4(warmed * intensity, tex.a);
          }
        `}
      />
    </mesh>
  );
}

// Subtle moving cloud layer above the globe
function Clouds({ cloudsImageUrl, speed = 0.0025, flipTextureVertically, flipTextureHorizontally }: { cloudsImageUrl: string; speed?: number; flipTextureVertically?: boolean; flipTextureHorizontally?: boolean }) {
  const { gl } = useThree();
  const [texture, setTexture] = useState<THREE.Texture | undefined>(undefined);
  const meshRef = useRef<THREE.Mesh>(null);
  useEffect(() => {
    let disposed = false;
    let loader: KTX2Loader | THREE.TextureLoader | undefined;
    let loadedTexture: THREE.Texture | undefined;
    const done = (tex: THREE.Texture) => {
      if (disposed) { try { tex.dispose(); } catch {} return; }
      tex.colorSpace = THREE.SRGBColorSpace;
      const hasMipmaps = Array.isArray(tex.mipmaps) && tex.mipmaps.length > 1;
      tex.generateMipmaps = false;
      tex.minFilter = hasMipmaps ? THREE.LinearMipmapLinearFilter : THREE.LinearFilter;
      tex.magFilter = THREE.LinearFilter;
      tex.wrapS = tex.wrapT = THREE.RepeatWrapping;
      const flipX = flipTextureHorizontally ?? false;
      const flipY = flipTextureVertically ?? false;
      tex.repeat.set(flipX ? -1 : 1, flipY ? -1 : 1);
      tex.offset.set(flipX ? 1 : 0, flipY ? 1 : 0);
      tex.needsUpdate = true;
      loadedTexture = tex;
      setTexture(tex);
    };
    if (cloudsImageUrl?.toLowerCase().endsWith('.ktx2')) {
      loader = getKtx2Loader(gl as unknown as THREE.WebGLRenderer);
      loader.load(cloudsImageUrl, done, undefined, () => setTexture(undefined));
    } else if (cloudsImageUrl) {
      loader = new THREE.TextureLoader();
      loader.load(cloudsImageUrl, done, undefined, () => setTexture(undefined));
    } else {
      setTexture(undefined);
    }
    return () => {
      disposed = true;
      try { loadedTexture?.dispose(); } catch {}
    };
  }, [cloudsImageUrl, gl, flipTextureVertically, flipTextureHorizontally]);
  useFrame((_s, delta) => {
    if (meshRef.current) {
      meshRef.current.rotation.y += speed * delta * 60; // approx frame-normalized
    }
  });
  if (!texture) return null;
  return (
    <mesh ref={meshRef}>
      <sphereGeometry args={[101.0, 48, 48]} />
      <meshPhongMaterial
        map={texture}
        transparent
        // Use normal alpha blending and a slightly lower opacity to prevent a dark/bright rim at the limb
        opacity={0.25}
        depthWrite={false}
        blending={THREE.NormalBlending}
        side={THREE.FrontSide}
      />
    </mesh>
  );
}