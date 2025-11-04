"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Canvas, useFrame, useLoader, useThree } from "@react-three/fiber";
import { Effects, Html, OrbitControls } from "@react-three/drei";
import ThreeGlobe from "three-globe";
import { UnrealBloomPass, KTX2Loader } from "three-stdlib";
import countries from "@/data/globe.json";
import {
  AdditiveBlending,
  BackSide,
  BufferAttribute,
  BufferGeometry,
  ClampToEdgeWrapping,
  Color,
  FrontSide,
  LinearFilter,
  LinearMipmapLinearFilter,
  MathUtils,
  Mesh,
  MeshPhongMaterial,
  RepeatWrapping,
  Scene,
  SRGBColorSpace,
  Texture,
  TextureLoader,
  Vector3,
  WebGLRenderer,
} from "three";
import type { IUniform, Uniforms } from "three";

type Position = {
  order: number;
  startLat: number;
  startLng: number;
  endLat: number;
  endLng: number;
  arcAlt: number;
  color: string;
};

type GlobePoint = {
  size: number;
  order: number;
  color: string;
  lat: number;
  lng: number;
};

export type GlobeConfig = {
  pointSize?: number;
  globeColor?: string;
  globeImageUrl?: string;
  bumpImageUrl?: string;
  specularImageUrl?: string;
  cloudsImageUrl?: string;
  cloudsSpeed?: number;
  showAtmosphere?: boolean;
  atmosphereColor?: string;
  atmosphereAltitude?: number;
  nightImageUrl?: string;
  showNightLights?: boolean;
  nightLightsStrength?: number;
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
  arcDensity?: number;
  maxArcs?: number;
  arcTime?: number;
  arcLength?: number;
  arcAnimate?: boolean;
  arcGap?: number;
  rings?: number;
  maxRings?: number;
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
  useSkybox?: boolean;
  starsBackgroundUrl?: string;
  starfieldCount?: number;
  enableBloom?: boolean;
  forceBloomInDev?: boolean;
  themeBlend?: number;
};

type ResolvedGlobeConfig = GlobeConfig & {
  pointSize: number;
  globeColor: string;
  showAtmosphere: boolean;
  atmosphereColor: string;
  atmosphereAltitude: number;
  polygonColor: string;
  cloudsSpeed: number;
  emissive: string;
  emissiveIntensity: number;
  shininess: number;
  arcDensity: number;
  arcTime: number;
  arcLength: number;
  arcAnimate: boolean;
  arcGap: number;
  rings: number;
  maxRings: number;
  showRings: boolean;
  flipPoles: boolean;
  flipTextureVertically: boolean;
  flipTextureHorizontally: boolean;
  flipLongitude: boolean;
  useSkybox: boolean;
  starfieldCount: number;
  enableBloom: boolean;
  forceBloomInDev: boolean;
  nightLightsStrength: number;
  themeBlend: number;
};

const DEFAULT_GLOBE_CONFIG: ResolvedGlobeConfig = {
  pointSize: 1,
  globeColor: "#1d072e",
  globeImageUrl: undefined,
  bumpImageUrl: undefined,
  specularImageUrl: undefined,
  nightImageUrl: "/earth_nightmap.jpg",
  nightLightsStrength: 1.2,
  cloudsImageUrl: undefined,
  cloudsSpeed: 0.0025,
  showAtmosphere: true,
  atmosphereColor: "#ffffff",
  atmosphereAltitude: 0.1,
  polygonColor: "rgba(255,255,255,0.7)",
  showHexPolygons: false,
  ambientLight: "#7fb1ff",
  ambientIntensity: 0.75,
  directionalLeftLight: "#f0f5ff",
  directionalTopLight: "#deebff",
  directionalIntensity: 0.72,
  pointLight: "#ffffff",
  emissive: "#000000",
  emissiveIntensity: 0.1,
  shininess: 20,
  arcDensity: 0.2,
  maxArcs: undefined,
  arcTime: 2000,
  arcLength: 0.25,
  arcAnimate: true,
  arcGap: 0.95,
  rings: 1,
  maxRings: 3,
  showRings: true,
  initialPosition: { lat: 0, lng: 0, altitude: 2.1 },
  autoRotate: true,
  autoRotateSpeed: 0.06,
  flipPoles: false,
  flipTextureVertically: false,
  flipTextureHorizontally: false,
  flipLongitude: false,
  useSkybox: true,
  starsBackgroundUrl: "/stars_milky.jpg",
  starfieldCount: 2000,
  enableBloom: true,
  forceBloomInDev: false,
  themeBlend: 0,
};

type BlendUniforms = Uniforms & {
  uDayMap: IUniform<Texture | null>;
  uNightMap: IUniform<Texture | null>;
  uMix: IUniform<number>;
};

const RING_PROPAGATION_SPEED = 3;

let sharedKtx2Loader: KTX2Loader | null = null;

function disposeTexture(texture: Texture | null | undefined) {
  if (texture) {
    texture.dispose();
  }
}

function getSharedKtx2Loader(renderer: WebGLRenderer): KTX2Loader {
  if (!sharedKtx2Loader) {
    sharedKtx2Loader = new KTX2Loader().setTranscoderPath("/basis/");
    sharedKtx2Loader.detectSupport(renderer);
  }
  return sharedKtx2Loader;
}

interface LoadTextureOptions {
  flipX?: boolean;
  flipY?: boolean;
  wrapMode?: THREE.Wrapping;
  repeat?: boolean;
  minFilter?: THREE.TextureFilter;
  magFilter?: THREE.TextureFilter;
}

function loadTextureResource(
  renderer: WebGLRenderer,
  url: string,
  options: LoadTextureOptions = {},
): Promise<Texture> {
  const flipX = options.flipX ?? false;
  const flipY = options.flipY ?? false;
  const wrapMode = options.repeat ? RepeatWrapping : options.wrapMode ?? ClampToEdgeWrapping;
  const desiredMinFilter = options.minFilter;
  const desiredMagFilter = options.magFilter;

  return new Promise<Texture>((resolve, reject) => {
    const finalize = (texture: Texture) => {
      texture.wrapS = wrapMode;
      texture.wrapT = wrapMode;
      texture.repeat.set(flipX ? -1 : 1, flipY ? -1 : 1);
      texture.offset.set(flipX ? 1 : 0, flipY ? 1 : 0);
      texture.colorSpace = SRGBColorSpace;
      const hasMipmaps = texture.mipmaps.length > 1;
      texture.generateMipmaps = false;
      texture.minFilter = desiredMinFilter ?? (hasMipmaps ? LinearMipmapLinearFilter : LinearFilter);
      texture.magFilter = desiredMagFilter ?? LinearFilter;
      texture.needsUpdate = true;
      resolve(texture);
    };

    if (url.toLowerCase().endsWith(".ktx2")) {
      try {
        const loader = getSharedKtx2Loader(renderer);
        loader.load(url, finalize, undefined, () => {
          reject(new Error(`Failed to load KTX2 texture: ${url}`));
        });
      } catch (error) {
        reject(error instanceof Error ? error : new Error("Failed to initialise KTX2 loader"));
      }
    } else {
      new TextureLoader().load(url, finalize, undefined, () => {
        reject(new Error(`Failed to load texture: ${url}`));
      });
    }
  });
}

interface WorldProps {
  globeConfig: GlobeConfig;
  data: Position[];
}

export function Globe({ globeConfig, data }: WorldProps) {
  const { gl } = useThree();
  const globeRef = useRef<ThreeGlobe | null>(null);
  const groupRef = useRef<THREE.Group | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);

  const dayTextureRef = useRef<Texture | null>(null);
  const nightTextureRef = useRef<Texture | null>(null);
  const bumpTextureRef = useRef<Texture | null>(null);
  const specularTextureRef = useRef<Texture | null>(null);
  const shaderUniformsRef = useRef<BlendUniforms | null>(null);
  const materialPatchedRef = useRef(false);
  const applyThemeBlendRef = useRef<(value: number) => void>(() => undefined);

  const mergedConfig = useMemo<ResolvedGlobeConfig>(
    () => ({
      ...DEFAULT_GLOBE_CONFIG,
      ...globeConfig,
      nightLightsStrength: globeConfig.nightLightsStrength ?? DEFAULT_GLOBE_CONFIG.nightLightsStrength,
      themeBlend: globeConfig.themeBlend ?? DEFAULT_GLOBE_CONFIG.themeBlend,
    }),
    [globeConfig],
  );

  useEffect(() => {
    if (!globeRef.current && groupRef.current) {
      globeRef.current = new ThreeGlobe();
      groupRef.current.add(globeRef.current);
      setIsInitialized(true);
    }
  }, []);

  useEffect(() => {
    if (!globeRef.current || !isInitialized) {
      return;
    }

    const material = globeRef.current.globeMaterial() as MeshPhongMaterial;
    const useTextureBase = Boolean(mergedConfig.globeImageUrl);
    material.color = new Color(useTextureBase ? "#ffffff" : mergedConfig.globeColor);
    material.emissive = new Color(mergedConfig.emissive);
    material.emissiveIntensity = mergedConfig.emissiveIntensity;
    material.shininess = mergedConfig.shininess;

    const flipX = mergedConfig.flipTextureHorizontally;
    const flipY = mergedConfig.flipTextureVertically;

    const updateBlendUniform = (value: number) => {
      if (shaderUniformsRef.current) {
        shaderUniformsRef.current.uMix.value = MathUtils.clamp(value, 0, 1);
      }
    };

    applyThemeBlendRef.current = updateBlendUniform;

    const ensureBlendShader = (dayTexture: Texture | null, nightTexture: Texture | null) => {
      if (!globeRef.current || !dayTexture) {
        return;
      }

      const activeNightTexture = nightTexture ?? dayTexture;
      material.map = dayTexture;
      material.defines = { ...(material.defines ?? {}), USE_MAP: "", USE_UV: "" };

      if (!materialPatchedRef.current) {
        material.onBeforeCompile = (shader) => {
          const uniforms = shader.uniforms as BlendUniforms;
          uniforms.uDayMap = { value: dayTexture } as IUniform<Texture | null>;
          uniforms.uNightMap = { value: activeNightTexture } as IUniform<Texture | null>;
          uniforms.uMix = { value: MathUtils.clamp(mergedConfig.themeBlend, 0, 1) } as IUniform<number>;

          if (!shader.fragmentShader.includes("uniform sampler2D uDayMap")) {
            shader.fragmentShader = `uniform sampler2D uDayMap;\nuniform sampler2D uNightMap;\nuniform float uMix;\n${shader.fragmentShader}`;
          }

          shader.fragmentShader = shader.fragmentShader.replace(
            "#include <map_fragment>",
            `#ifdef USE_MAP\n  vec4 texelColorDay = texture( uDayMap, vUv );\n  vec4 texelColorNight = texture( uNightMap, vUv );\n  vec3 dayLinear = pow( max( texelColorDay.rgb, vec3( 0.0001 ) ), vec3( 2.0 ) );\n  vec3 nightLinear = pow( max( texelColorNight.rgb, vec3( 0.0001 ) ), vec3( 2.0 ) );\n  float mixAmount = clamp( uMix, 0.0, 1.0 );\n  vec3 softenedNight = max( nightLinear, dayLinear * 0.32 );\n  vec3 blendedLinear = mix( dayLinear, softenedNight, mixAmount );\n  blendedLinear = mix( blendedLinear, normalize( blendedLinear + vec3( 0.08 ) ), mixAmount * 0.35 );\n  float alphaBlend = mix( texelColorDay.a, texelColorNight.a, mixAmount );\n  vec4 texelColor = vec4( blendedLinear, alphaBlend );\n  diffuseColor *= texelColor;\n#else\n  vec4 texelColor = vec4( 1.0 );\n#endif`,
          );

          shaderUniformsRef.current = uniforms;
        };

        material.customProgramCacheKey = () => `globe-mix-${mergedConfig.nightImageUrl ? 1 : 0}`;
        material.needsUpdate = true;
        materialPatchedRef.current = true;
      }

      if (shaderUniformsRef.current) {
        shaderUniformsRef.current.uDayMap.value = dayTexture;
        shaderUniformsRef.current.uNightMap.value = activeNightTexture;
        shaderUniformsRef.current.uMix.value = MathUtils.clamp(mergedConfig.themeBlend, 0, 1);
      }
    };

    let cancelled = false;

    const assignDayTexture = async () => {
      if (!mergedConfig.globeImageUrl) {
        disposeTexture(dayTextureRef.current);
        dayTextureRef.current = null;
        ensureBlendShader(null, nightTextureRef.current);
        return;
      }

      try {
        const texture = await loadTextureResource(gl, mergedConfig.globeImageUrl, { flipX, flipY });
        if (cancelled) {
          disposeTexture(texture);
          return;
        }
        disposeTexture(dayTextureRef.current);
        dayTextureRef.current = texture;
        ensureBlendShader(texture, nightTextureRef.current);
      } catch (error) {
        if (!cancelled) {
          globeRef.current?.globeImageUrl(mergedConfig.globeImageUrl);
        }
      }
    };

    const assignNightTexture = async () => {
      if (!mergedConfig.nightImageUrl) {
        disposeTexture(nightTextureRef.current);
        nightTextureRef.current = null;
        ensureBlendShader(dayTextureRef.current, null);
        return;
      }

      try {
        const texture = await loadTextureResource(gl, mergedConfig.nightImageUrl, { flipX, flipY });
        if (cancelled) {
          disposeTexture(texture);
          return;
        }
        disposeTexture(nightTextureRef.current);
        nightTextureRef.current = texture;
        ensureBlendShader(dayTextureRef.current, texture);
      } catch {
        if (!cancelled) {
          disposeTexture(nightTextureRef.current);
          nightTextureRef.current = null;
          ensureBlendShader(dayTextureRef.current, null);
        }
      }
    };

    const assignBumpTexture = async () => {
      if (!mergedConfig.bumpImageUrl) {
        disposeTexture(bumpTextureRef.current);
        bumpTextureRef.current = null;
        material.bumpMap = null;
        material.needsUpdate = true;
        return;
      }

      try {
        const texture = await loadTextureResource(gl, mergedConfig.bumpImageUrl, { flipX, flipY });
        if (cancelled) {
          disposeTexture(texture);
          return;
        }
        disposeTexture(bumpTextureRef.current);
        bumpTextureRef.current = texture;
        material.bumpMap = texture;
        material.bumpScale = 0.4;
        material.needsUpdate = true;
      } catch {
        globeRef.current?.bumpImageUrl(mergedConfig.bumpImageUrl!);
      }
    };

    const assignSpecularTexture = async () => {
      if (!mergedConfig.specularImageUrl) {
        disposeTexture(specularTextureRef.current);
        specularTextureRef.current = null;
        material.specularMap = null;
        material.needsUpdate = true;
        return;
      }

      try {
        const texture = await loadTextureResource(gl, mergedConfig.specularImageUrl, { flipX, flipY });
        if (cancelled) {
          disposeTexture(texture);
          return;
        }
        disposeTexture(specularTextureRef.current);
        specularTextureRef.current = texture;
        material.specularMap = texture;
        material.specular = new Color(0x444444);
        material.needsUpdate = true;
      } catch {
        globeRef.current?.specularImageUrl(mergedConfig.specularImageUrl!);
      }
    };

    assignDayTexture();
    assignNightTexture();
    assignBumpTexture();
    assignSpecularTexture();

    return () => {
      cancelled = true;
    };
  }, [
    gl,
    isInitialized,
    mergedConfig.arcAnimate,
    mergedConfig.bumpImageUrl,
    mergedConfig.emissive,
    mergedConfig.emissiveIntensity,
    mergedConfig.flipTextureHorizontally,
    mergedConfig.flipTextureVertically,
    mergedConfig.globeColor,
    mergedConfig.globeImageUrl,
    mergedConfig.nightImageUrl,
    mergedConfig.specularImageUrl,
    mergedConfig.themeBlend,
    mergedConfig.shininess,
  ]);

  useEffect(() => {
    applyThemeBlendRef.current(mergedConfig.themeBlend);
  }, [mergedConfig.themeBlend]);

  useEffect(() => {
    if (!globeRef.current || !isInitialized) {
      return;
    }

    const { lat, lng, altitude } = mergedConfig.initialPosition ?? DEFAULT_GLOBE_CONFIG.initialPosition!;
    const altitudeValue = altitude ?? DEFAULT_GLOBE_CONFIG.initialPosition!.altitude!;

    requestAnimationFrame(() => {
      globeRef.current?.pointOfView({ lat, lng, altitude: altitudeValue }, 0);
    });

    if (groupRef.current) {
      groupRef.current.rotation.set(0, 0, 0);
      if (mergedConfig.flipLongitude) {
        groupRef.current.rotation.y = Math.PI;
      }
      if (mergedConfig.flipPoles) {
        groupRef.current.rotation.z = Math.PI;
      }
    }
  }, [
    isInitialized,
    mergedConfig.initialPosition?.altitude,
    mergedConfig.initialPosition?.lat,
    mergedConfig.initialPosition?.lng,
    mergedConfig.flipLongitude,
    mergedConfig.flipPoles,
  ]);

  useEffect(() => {
    if (!globeRef.current || !isInitialized || data.length === 0) {
      return;
    }

    const density = Math.max(0, Math.min(1, mergedConfig.arcDensity));
    const step = density === 0 ? Number.POSITIVE_INFINITY : Math.max(1, Math.round(1 / density));
    const sampledArcs = density === 0 ? [] : data.filter((_, index) => index % step === 0);
    const arcs = mergedConfig.maxArcs != null ? sampledArcs.slice(0, mergedConfig.maxArcs) : sampledArcs;

    const points: GlobePoint[] = arcs.flatMap((arc) => [
      {
        size: mergedConfig.pointSize,
        order: arc.order,
        color: arc.color,
        lat: arc.startLat,
        lng: arc.startLng,
      },
      {
        size: mergedConfig.pointSize,
        order: arc.order,
        color: arc.color,
        lat: arc.endLat,
        lng: arc.endLng,
      },
    ]);

    const seenPoints = new Set<string>();
    const filteredPoints = points.filter((point) => {
      const key = `${point.lat.toFixed(4)}|${point.lng.toFixed(4)}`;
      if (seenPoints.has(key)) {
        return false;
      }
      seenPoints.add(key);
      return true;
    });

    const globe = globeRef.current;

    if (mergedConfig.showHexPolygons) {
      globe
        .hexPolygonsData(countries.features)
        .hexPolygonResolution(3)
        .hexPolygonMargin(0.7)
        .showAtmosphere(mergedConfig.showAtmosphere)
        .atmosphereColor(mergedConfig.atmosphereColor)
        .atmosphereAltitude(mergedConfig.atmosphereAltitude)
        .hexPolygonColor(() => mergedConfig.polygonColor);
    } else {
      globe
        .hexPolygonsData([])
        .showAtmosphere(mergedConfig.showAtmosphere)
        .atmosphereColor(mergedConfig.atmosphereColor)
        .atmosphereAltitude(mergedConfig.atmosphereAltitude);
    }

    globe
      .arcsData(arcs)
      .arcStartLat((d: Position) => d.startLat)
      .arcStartLng((d: Position) => d.startLng)
      .arcEndLat((d: Position) => d.endLat)
      .arcEndLng((d: Position) => d.endLng)
      .arcColor((d: Position) => [d.color, "rgba(255,255,255,0)"])
      .arcAltitude((d: Position) => d.arcAlt)
      .arcStroke(() => 1)
      .arcDashLength(mergedConfig.arcAnimate ? Math.min(Math.max(mergedConfig.arcLength, 0.01), 0.99) : 1)
      .arcDashInitialGap((d: Position) => (mergedConfig.arcAnimate ? d.order : 0))
      .arcDashGap(mergedConfig.arcAnimate ? Math.min(Math.max(mergedConfig.arcGap, 0), 2) : 0)
      .arcDashAnimateTime(mergedConfig.arcAnimate ? mergedConfig.arcTime : 0);

    globe
      .pointsData(filteredPoints)
      .pointColor((point: GlobePoint) => point.color)
      .pointsMerge(true)
      .pointAltitude(0)
      .pointRadius(0.25);

    globe
      .ringsData([])
      .ringColor(() => mergedConfig.polygonColor)
      .ringMaxRadius(mergedConfig.maxRings)
      .ringPropagationSpeed(RING_PROPAGATION_SPEED)
      .ringRepeatPeriod((mergedConfig.arcTime * mergedConfig.arcLength) / Math.max(mergedConfig.rings, 1));
  }, [
    data,
    isInitialized,
    mergedConfig.arcAnimate,
    mergedConfig.arcDensity,
    mergedConfig.arcGap,
    mergedConfig.arcLength,
    mergedConfig.arcTime,
    mergedConfig.atmosphereAltitude,
    mergedConfig.atmosphereColor,
    mergedConfig.maxArcs,
    mergedConfig.maxRings,
    mergedConfig.pointSize,
    mergedConfig.polygonColor,
    mergedConfig.rings,
    mergedConfig.showAtmosphere,
    mergedConfig.showHexPolygons,
  ]);

  useEffect(() => {
    if (!globeRef.current || !isInitialized || data.length === 0) {
      return;
    }

    if (!mergedConfig.showRings) {
      globeRef.current.ringsData([]);
      return;
    }

    const interval = window.setInterval(() => {
      if (!globeRef.current) {
        return;
      }

      const sampleSize = Math.floor((data.length * 4) / 5);
      const indices = genRandomNumbers(0, data.length, sampleSize);

      const ringsData = data
        .filter((_, index) => indices.includes(index))
        .map((entry) => ({
          lat: entry.startLat,
          lng: entry.startLng,
          color: entry.color,
        }));

      globeRef.current.ringsData(ringsData);
    }, 2000);

    return () => {
      window.clearInterval(interval);
    };
  }, [data, isInitialized, mergedConfig.showRings]);

  useEffect(() => () => {
    disposeTexture(dayTextureRef.current);
    disposeTexture(nightTextureRef.current);
    disposeTexture(bumpTextureRef.current);
    disposeTexture(specularTextureRef.current);
    shaderUniformsRef.current = null;
    materialPatchedRef.current = false;
  }, []);

  return <group ref={groupRef} />;
}

function WebGLRendererConfig() {
  const { gl, size } = useThree();

  useEffect(() => {
    gl.setPixelRatio(1);
    gl.setSize(size.width, size.height);
    gl.setClearColor(0x000010, 1);

    const canvas = gl.domElement as HTMLCanvasElement;

    const onLost = (event: WebGLContextEvent) => {
      event.preventDefault();
    };
    const onRestored = () => undefined;

    canvas.addEventListener("webglcontextlost", onLost, false);
    canvas.addEventListener("webglcontextrestored", onRestored, false);

    return () => {
      canvas.removeEventListener("webglcontextlost", onLost, false);
      canvas.removeEventListener("webglcontextrestored", onRestored, false);
    };
  }, [gl, size.height, size.width]);

  return null;
}

export function World(props: WorldProps) {
  const { globeConfig } = props;
  const scene = useMemo(() => {
    const customScene = new Scene();
    customScene.fog = null;
    return customScene;
  }, []);

  const bloomPass = useMemo(() => {
    const pass = new UnrealBloomPass();
    pass.threshold = 0.9;
    pass.strength = 0.4;
    pass.radius = 0.4;
    return pass;
  }, []);

  const nightLightsIntensity = useMemo(
    () =>
      (globeConfig.nightLightsStrength ?? DEFAULT_GLOBE_CONFIG.nightLightsStrength) *
      MathUtils.clamp(globeConfig.themeBlend ?? 0, 0, 1),
    [globeConfig.nightLightsStrength, globeConfig.themeBlend],
  );

  const cloudOpacity = useMemo(
    () => Math.max(0.08, 0.35 - 0.27 * MathUtils.clamp(globeConfig.themeBlend ?? 0, 0, 1)),
    [globeConfig.themeBlend],
  );

  return (
    <Canvas
      scene={scene}
      camera={{ fov: 50, near: 180, far: 1800, position: [0, 0, 300] }}
      dpr={[1, 1]}
      gl={{
        powerPreference: "high-performance",
        antialias: false,
        alpha: false,
        stencil: false,
        premultipliedAlpha: false,
        preserveDrawingBuffer: false,
      }}
    >
      <WebGLRendererConfig />
      <ambientLight color={globeConfig.ambientLight} intensity={globeConfig.ambientIntensity ?? 0.35} />
      <directionalLight
        color={globeConfig.directionalLeftLight}
        position={new Vector3(-400, 100, 400)}
        intensity={globeConfig.directionalIntensity ?? 1.5}
      />
      {globeConfig.useSkybox ? (
        <SkySphere textureUrl={globeConfig.starsBackgroundUrl} />
      ) : (
        <Starfield count={globeConfig.starfieldCount ?? DEFAULT_GLOBE_CONFIG.starfieldCount} />
      )}
      {globeConfig.showAtmosphere !== false && <Atmosphere color="#66a6ff" intensity={0.25} radius={102} />}
      {globeConfig.showNightLights !== false && globeConfig.nightImageUrl && (
        <NightLights
          nightImageUrl={globeConfig.nightImageUrl}
          lightDir={new Vector3(-400, 100, 400)}
          flipTextureVertically={globeConfig.flipTextureVertically}
          flipTextureHorizontally={globeConfig.flipTextureHorizontally}
          intensity={nightLightsIntensity}
        />
      )}
      {globeConfig.cloudsImageUrl && (
        <Clouds
          cloudsImageUrl={globeConfig.cloudsImageUrl}
          speed={globeConfig.cloudsSpeed ?? DEFAULT_GLOBE_CONFIG.cloudsSpeed}
          flipTextureVertically={globeConfig.flipTextureVertically}
          flipTextureHorizontally={globeConfig.flipTextureHorizontally}
          opacity={cloudOpacity}
        />
      )}
      <Globe {...props} />
      <HoverMarkers data={props.data} />
      {globeConfig.enableBloom !== false &&
        (process.env.NODE_ENV !== "development" || globeConfig.forceBloomInDev) && (
          <Effects disableGamma>
            <primitive object={bloomPass} />
          </Effects>
        )}
      <OrbitControls
        enablePan={false}
        enableZoom
        minDistance={220}
        maxDistance={600}
        autoRotateSpeed={globeConfig.autoRotateSpeed ?? DEFAULT_GLOBE_CONFIG.autoRotateSpeed}
        autoRotate={globeConfig.autoRotate !== false}
        enableDamping
        dampingFactor={0.06}
        minPolarAngle={Math.PI / 3.5}
        maxPolarAngle={Math.PI - Math.PI / 3}
      />
    </Canvas>
  );
}

function Atmosphere({
  color = "#66a6ff",
  intensity = 0.25,
  radius = 102,
}: {
  color?: string;
  intensity?: number;
  radius?: number;
}) {
  const glowColor = new Color(color);
  const uniforms: Record<string, IUniform> = {
    glowColor: { value: new Vector3(glowColor.r, glowColor.g, glowColor.b) },
    glowIntensity: { value: intensity },
  };

  return (
    <mesh>
      <sphereGeometry args={[radius, 48, 48]} />
      <shaderMaterial
        transparent
        depthWrite={false}
        blending={AdditiveBlending}
        side={BackSide}
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

function Starfield({ count = 2000, radius = 1200 }: { count?: number; radius?: number }) {
  const geometryRef = useRef<BufferGeometry | null>(null);

  useEffect(() => {
    const geometry = geometryRef.current;
    if (!geometry) {
      return;
    }

``` and so on
